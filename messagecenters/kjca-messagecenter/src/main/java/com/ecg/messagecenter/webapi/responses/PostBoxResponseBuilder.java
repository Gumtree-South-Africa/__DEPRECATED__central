package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import java.util.List;

public class PostBoxResponseBuilder {

    private final RiakConversationBlockRepository conversationBlockRepository;
    private final static int MAX_UNREAD_NUMBER = 30;

    public PostBoxResponseBuilder(RiakConversationBlockRepository conversationBlockRepository) {
        this.conversationBlockRepository = conversationBlockRepository;
    }

    public ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, PostBox postBox, boolean newCounterMode) {
        return buildPostBoxResponse(email, size, page, null, postBox, newCounterMode);
    }

    public ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, ConversationRole role, PostBox postBox, boolean newCounterMode) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        if (newCounterMode) {
            postBoxResponse.initNumUnread((int) postBox.getNewRepliesCounter().getValue(), null, null, postBox.getLastModification());
        } else {
            postBoxResponse.initNumUnread(postBox.getUnreadConversationsCapped().size(), getNumUnreadForRole(email, ConversationRole.Buyer, postBox, MAX_UNREAD_NUMBER), getNumUnreadForRole(email, ConversationRole.Seller, postBox, MAX_UNREAD_NUMBER), postBox.getLastModification());
        }

        initConversationsPayload(email, postBox.getFilteredConversationThreads(roleFilter(role, email), page, size), postBoxResponse);

        int filteredSize = FluentIterable.from(postBox.getConversationThreads())
                .filter(roleFilter(role, email)).size();
        postBoxResponse.meta(filteredSize, page, size);

        return ResponseObject.of(postBoxResponse);
    }

    int getNumUnreadForRole(String email, ConversationRole role, PostBox postBox, int maxNum) {
        return Math.toIntExact(
                postBox.getConversationThreads().stream()
                        .filter(thread -> ((AbstractConversationThread) thread).isContainsUnreadMessages())
                        .filter(thread -> ConversationBoundnessFinder.lookupUsersRole(email, (AbstractConversationThread) thread) == role)
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

    private Predicate<AbstractConversationThread> roleFilter(ConversationRole role, String email){
        return conversationThread -> {
            if (role == null) {
                return true;
            }
            return role == ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
        };
    }
}
