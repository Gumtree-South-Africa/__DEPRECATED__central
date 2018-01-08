package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class PostBoxResponseBuilder {

    private final ConversationBlockRepository conversationBlockRepository;
    private final static int MAX_UNREAD_NUMBER = 30;

    private int maxAgeDays;

    public PostBoxResponseBuilder(ConversationBlockRepository conversationBlockRepository, int maxAgeDays) {
        this.conversationBlockRepository = conversationBlockRepository;
        this.maxAgeDays = maxAgeDays;
    }

    public ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, PostBox postBox, boolean newCounterMode) {
        return buildPostBoxResponse(email, size, page, null, postBox, newCounterMode);
    }

    public ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, ConversationRole role, PostBox postBox, boolean newCounterMode) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        if (newCounterMode) {
            // not used. should be removed.
            postBoxResponse.initNumUnread((int) postBox.getNewRepliesCounter().getValue(), null, null, postBox.getLastModification());
        } else {
            final List<AbstractConversationThread> unreadThreads =
                    (List<AbstractConversationThread>) postBox.getUnreadConversations().values()
                            .stream()
                            .filter(getLiveConversationsFilter())
                            .collect(Collectors.toList());
            postBoxResponse.initNumUnread(unreadThreads.size(),
                    getNumForRole(unreadThreads, email, ConversationRole.Buyer, PostBox.MAX_CONVERSATIONS_IN_POSTBOX),
                    getNumForRole(unreadThreads, email, ConversationRole.Seller, PostBox.MAX_CONVERSATIONS_IN_POSTBOX),
                    postBox.getLastModification());
        }

        List notExpiredConversations = new ArrayList<ConversationThread>(postBox.getConversationThreads())
                .stream()
                .filter(getLiveConversationsFilter())
                .collect(Collectors.toList());
        PostBox notExpiredConversationsPostBox = new PostBox(postBox.getEmail(), postBox.getNewRepliesCounter(), notExpiredConversations);
        initConversationsPayload(email, notExpiredConversationsPostBox.getFilteredConversationThreads(roleFilter(role, email), page, size), postBoxResponse);

        int filteredSize = (int) notExpiredConversationsPostBox.getConversationThreads().stream()
                .filter(roleFilter(role, email))
                .count();

        postBoxResponse.meta(filteredSize, page, size);

        return ResponseObject.of(postBoxResponse);
    }

    private Predicate<ConversationThread> getLiveConversationsFilter() {
        return conversation -> {
            LocalDate modificationDate = LocalDate.of(conversation.getModifiedAt().getYear(), conversation.getModifiedAt().getMonthOfYear(), conversation.getModifiedAt().getDayOfMonth());
            return DAYS.between(modificationDate, LocalDate.now()) <= maxAgeDays;
        };
    }

    private int getNumForRole(List<AbstractConversationThread> threads, String email, ConversationRole role, int maxNum) {
        return Math.toIntExact(
                threads.stream()
                        .filter(thread -> ConversationBoundnessFinder.lookupUsersRole(email, thread) == role)
                        .limit(maxNum)
                        .count()
        );
    }


    private void initConversationsPayload(String email, List<ConversationThread> conversationThreads, PostBoxResponse postBoxResponse) {
        for (ConversationThread conversationThread : conversationThreads) {
            postBoxResponse.addItem(createSinglePostBoxItem(email, conversationThread));
        }
    }


    private PostBoxListItemResponse createSinglePostBoxItem(String email, ConversationThread conversationThread) {
        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationThread.getConversationId());

        return new PostBoxListItemResponse(email, conversationThread, conversationBlock);
    }

    private Predicate<AbstractConversationThread> roleFilter(ConversationRole role, String email) {
        return conversationThread -> role == null || role == ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
    }
}
