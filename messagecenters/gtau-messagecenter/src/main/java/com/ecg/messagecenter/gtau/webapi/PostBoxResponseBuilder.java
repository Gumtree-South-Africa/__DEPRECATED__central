package com.ecg.messagecenter.gtau.webapi;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.gtau.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.gtau.util.ConversationThreadEnricher;
import com.ecg.messagecenter.gtau.util.MessagesResponseFactory;
import com.ecg.messagecenter.gtau.webapi.responses.MessageResponse;
import com.ecg.messagecenter.gtau.webapi.responses.PostBoxListItemResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.sync.PostBoxResponse;

import java.util.List;
import java.util.Optional;

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
