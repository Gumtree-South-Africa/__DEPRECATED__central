package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;

import java.util.List;
import java.util.Optional;

public class PostBoxResponseBuilder {

    private static final Counter LIST_AGGREGATE_HIT = TimingReports.newCounter("message-box.list-aggregate-hit");
    private static final Counter LIST_AGGREGATE_MISS = TimingReports.newCounter("message-box.list-aggregate-miss");

    private final ConversationRepository conversationRepository;

    public PostBoxResponseBuilder(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page, PostBox postBox) {
        PostBoxResponse postBoxResponse = new PostBoxResponse();

        postBoxResponse.initNumUnread((int) postBox.getNewRepliesCounter().getValue(), postBox.getLastModification());

        initConversationsPayload(email, postBox.getConversationThreadsCapTo(page, size), postBoxResponse);

        postBoxResponse.meta(postBox.getConversationThreads().size(), page, size);

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
