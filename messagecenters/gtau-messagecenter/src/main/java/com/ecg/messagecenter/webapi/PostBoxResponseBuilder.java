package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.cleanup.TextCleaner;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.Header;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class PostBoxResponseBuilder {

    private static final Counter LIST_AGGREGATE_HIT = TimingReports.newCounter("message-box.list-aggregate-hit");
    private static final Counter LIST_AGGREGATE_MISS = TimingReports.newCounter("message-box.list-aggregate-miss");

    private final ConversationRepository conversationRepository;

    public PostBoxResponseBuilder(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, PostBox postBox, boolean newCounterMode) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        if (newCounterMode) {
            postBoxResponse.initNumUnread(postBox.getNewRepliesCounter().getValue().intValue(), postBox.getLastModification());
        } else {
            postBoxResponse.initNumUnread(postBox.getUnreadConversationsCapped().size(), postBox.getLastModification());
        }

        initConversationsPayload(email, postBox.getConversationThreadsCapTo(page, size), postBoxResponse);

        postBoxResponse.meta(postBox.getConversationThreads().size(), page, size);

        return ResponseObject.of(postBoxResponse);
    }

    ResponseObject<PostBoxResponse> buildPostBoxResponseRobotExcluded(String email, int size, int page,PostBox postBox, boolean newCounterMode) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();
        Conversation conversation;
        List<ConversationThread> normalConversationThreads = Lists.newArrayList();
        for(ConversationThread conversationThread : conversationThreads) {
            conversation = conversationRepository.getById(conversationThread.getConversationId());
            if(conversation == null) {
                continue;
            }

            List<Message> normalMessages = Lists.newArrayList();
            // for this conversation only include sent messages that are non-robot
            for(Message message : conversation.getMessages()) {
                if(message.getHeaders().get(Header.Robot.getValue()) == null && message.getState().equals(MessageState.SENT)) {
                    normalMessages.add(message);
                }
            }

            if (!normalMessages.isEmpty()) {
                Message lastMessage = Iterables.getLast(normalMessages);
                normalConversationThreads.add(new ConversationThread(
                        conversationThread.getAdId(),
                        conversationThread.getConversationId(),
                        conversationThread.getCreatedAt(),
                        lastMessage.getLastModifiedAt(),
                        lastMessage.getLastModifiedAt(),
                        conversationThread.isContainsUnreadMessages(),
                        Optional.of(MessageCenterUtils.truncateText(TextCleaner.cleanupText(lastMessage.getPlainTextBody()), 250)),
                        conversationThread.getBuyerName(),
                        conversationThread.getSellerName(),
                        conversationThread.getBuyerId(),
                        conversationThread.getMessageDirection(),
                        Optional.<String>empty(),
                        Optional.<String>empty()));
            }
        }

        List<ConversationThread> normalUnreadConversationThreadsCapped = FluentIterable.from(postBox.getUnreadConversations().values())
                .filter(new Predicate<ConversationThread>() {
                    @Override
                    public boolean apply(@Nullable ConversationThread input) {
                        return input.isContainsUnreadMessages();
                    }
                })
                .skip(page * size)
                .limit(size)
                .toList();

        List<ConversationThread> normalConversationThreadsCapped = FluentIterable.from(normalConversationThreads)
                .skip(page * size)
                .limit(size)
                .toList();

        postBoxResponse.initNumUnread(normalUnreadConversationThreadsCapped.size(), postBox.getLastModification());

        initConversationsPayload(email, normalConversationThreadsCapped, postBoxResponse);

        postBoxResponse.meta(normalConversationThreads.size(), page, size);

        return ResponseObject.of(postBoxResponse);
    }


    private void initConversationsPayload(String email, List<ConversationThread> conversationThreads, PostBoxResponse postBoxResponse) {


        for (ConversationThread conversationThread : conversationThreads) {
            Optional<PostBoxListItemResponse> singlePostBoxItem = createSinglePostBoxItem(email, conversationThread);
            // postbox + conversation buckets are decoupled -> possibility of getting out of sync
            if (singlePostBoxItem.isPresent()) {
                postBoxResponse.addItem(singlePostBoxItem.get());
            }
        }
    }


    private Optional<PostBoxListItemResponse> createSinglePostBoxItem(String email, ConversationThread conversationThread) {
        if (conversationThread.containsNewListAggregateData()) {
            LIST_AGGREGATE_HIT.inc();
            return Optional.of(new PostBoxListItemResponse(email, conversationThread));
        }

        Conversation conversation = conversationRepository.getById(conversationThread.getConversationId());

        if (conversation == null) {
            return Optional.empty();
        }

        LIST_AGGREGATE_MISS.inc();

        return PostBoxListItemResponse.createNonAggregateListViewItem(email,
                conversationThread.isContainsUnreadMessages(),
                conversation);
    }



}
