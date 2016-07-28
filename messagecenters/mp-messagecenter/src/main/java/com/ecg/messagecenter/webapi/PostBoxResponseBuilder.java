package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class PostBoxResponseBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxResponseBuilder.class);

    private static final Counter LIST_AGGREGATE_HIT = TimingReports.newCounter("message-box.list-aggregate-hit");
    private static final Counter LIST_AGGREGATE_MISS = TimingReports.newCounter("message-box.list-aggregate-miss");

    private ConversationRepository conversationRepository;
    private UserIdentifierService userIdentifierService;

    public PostBoxResponseBuilder(ConversationRepository conversationRepository, UserIdentifierService userIdentifierService) {
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
    }

    public PostBoxResponse buildPostBoxResponse(String userId, int size, int page, PostBox postBox) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        postBoxResponse.initNumUnreadMessages(postBox.getNumUnreadMessages());

        initConversationsPayload(userId, postBox.getConversationThreadsCapTo(page, size), postBoxResponse);

        postBoxResponse.meta(postBoxResponse.getConversations().size(), page, size);

        return postBoxResponse;
    }

    private void initConversationsPayload(String userId, List<ConversationThread> conversationThreads, PostBoxResponse postBoxResponse) {
        for (ConversationThread conversationThread : conversationThreads) {
            Optional<PostBoxListItemResponse> singlePostBoxItem = createSinglePostBoxItem(userId, conversationThread);
            // postbox + conversation buckets are decoupled -> possibility of getting out of sync
            if (singlePostBoxItem.isPresent()) {
                postBoxResponse.addItem(singlePostBoxItem.get());
            }
        }
    }

    private Optional<PostBoxListItemResponse> createSinglePostBoxItem(String userId, ConversationThread conversationThread) {
        if (conversationThread.containsNewListAggregateData()) {
            LIST_AGGREGATE_HIT.inc();
            return Optional.of(new PostBoxListItemResponse(userId, conversationThread, userIdentifierService));
        }

        Conversation conversation = conversationRepository.getById(conversationThread.getConversationId());

        if (conversation == null) {
            return Optional.empty();
        }

        LIST_AGGREGATE_MISS.inc();

        LOGGER.warn("List aggregate miss for userId {} and conversation id {}", userId, conversationThread.getConversationId());

        return PostBoxListItemResponse.createNonAggregateListViewItem(userId,
                conversationThread.getNumUnreadMessages(),
                conversation,
                userIdentifierService);
    }
}