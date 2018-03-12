package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.cleanup.TextCleaner;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.Header;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.util.ConversationThreadEnricher;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.sync.PostBoxResponse;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostBoxResponseBuilder {

    private static final Counter LIST_AGGREGATE_HIT = TimingReports.newCounter("message-box.list-aggregate-hit");
    private static final Counter LIST_AGGREGATE_MISS = TimingReports.newCounter("message-box.list-aggregate-miss");
    private static final Timer INIT_CONVERSATION_PAYLOAD_TIMER = TimingReports.newTimer("message-box.init-conversation-payload-timer");

    private final ConversationRepository conversationRepository;
    private final ConversationThreadEnricher conversationThreadEnricher;

    public PostBoxResponseBuilder(ConversationRepository conversationRepository, ConversationThreadEnricher conversationThreadEnricher) {
        this.conversationRepository = conversationRepository;
        this.conversationThreadEnricher = conversationThreadEnricher;
    }

    public ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, PostBox postBox, boolean newCounterMode) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        if (newCounterMode) {
            postBoxResponse.initNumUnread((int) postBox.getNewRepliesCounter().getValue());
        } else {
            postBoxResponse.initNumUnread(postBox.getUnreadConversationsCapped().size());
        }
        postBoxResponse.initLastModified(postBox.getLastModification());

        initConversationsPayload(email, postBox.getConversationThreadsCapTo(page, size), postBoxResponse);
        postBoxResponse.meta(postBox.getConversationThreads().size(), page, size);
        return ResponseObject.of(postBoxResponse);
    }

    public ResponseObject<PostBoxResponse> buildPostBoxResponseRobotExcluded(String email, int size, int page, PostBox postBox) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();
        Conversation conversation;
        List<ConversationThread> normalConversationThreads = new ArrayList<>();
        for (ConversationThread conversationThread : conversationThreads) {
            conversation = conversationRepository.getById(conversationThread.getConversationId());
            if (conversation == null) {
                continue;
            }

            List<Message> normalMessages = conversation.getMessages().stream()
                    .filter(message -> message.getHeaders().get(Header.Robot.getValue()) == null && message.getState().equals(MessageState.SENT))
                    .collect(Collectors.toList());

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
                        conversationThread.getSellerId(),
                        conversationThread.getMessageDirection(),
                        Optional.empty(),
                        Optional.empty(),
                        lastMessage.getAttachmentFilenames(),
                        conversationThread.getLastMessageId(),
                        conversationThread.getBuyerAnonymousEmail(),
                        conversationThread.getSellerAnonymousEmail(),
                        conversationThread.getStatus()
                ));
            }
        }

        Map<String, ConversationThread> unreadConversations = postBox.getUnreadConversations();
        List<ConversationThread> normalUnreadConversationThreadsCapped = unreadConversations.values().stream()
                .filter(AbstractConversationThread::isContainsUnreadMessages)
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());

        List<ConversationThread> normalConversationThreadsCapped = normalConversationThreads.stream()
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());

        postBoxResponse.initNumUnread(normalUnreadConversationThreadsCapped.size());
        postBoxResponse.initLastModified(postBox.getLastModification());
        initConversationsPayload(email, normalConversationThreadsCapped, postBoxResponse);
        postBoxResponse.meta(normalConversationThreads.size(), page, size);

        return ResponseObject.of(postBoxResponse);
    }

    private void initConversationsPayload(String email, List<ConversationThread> conversationThreads, PostBoxResponse postBoxResponse) {
        try (Timer.Context ignored = INIT_CONVERSATION_PAYLOAD_TIMER.time()) {
            conversationThreads.forEach(conversationThread -> createSinglePostBoxItem(email, conversationThread).ifPresent(postBoxResponse::addItem));
        }
    }

    private Optional<PostBoxListItemResponse> createSinglePostBoxItem(String email, ConversationThread conversationThread) {
        Conversation conversation = conversationRepository.getById(conversationThread.getConversationId());
        if (conversation != null) {
            conversationThread = conversationThreadEnricher.enrichOnRead(conversationThread, conversation);
        }

        if (conversationThread.containsNewListAggregateData()) {
            LIST_AGGREGATE_HIT.inc();
            return Optional.of(new PostBoxListItemResponse(email, conversationThread));
        }

        if (conversation == null) {
            return Optional.empty();
        }

        LIST_AGGREGATE_MISS.inc();

        Optional<MessageResponse> messageResponse = MessagesResponseFactory.latestMessage(email, conversation);
        return messageResponse.isPresent()
                ? Optional.of(new PostBoxListItemResponse(email, conversation, conversationThread, messageResponse.get()))
                : Optional.empty();
    }
}
