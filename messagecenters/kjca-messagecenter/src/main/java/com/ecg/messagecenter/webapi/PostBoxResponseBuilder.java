package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockRepository;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;

import java.util.List;

public class PostBoxResponseBuilder {

    private final RiakConversationBlockRepository conversationBlockRepository;

    public PostBoxResponseBuilder(RiakConversationBlockRepository conversationBlockRepository) {
        this.conversationBlockRepository = conversationBlockRepository;
    }

    ResponseObject<PostBoxResponse> buildPostBoxResponse(String email, int size, int page,PostBox postBox, boolean newCounterMode) {
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


    private void initConversationsPayload(String email, List<ConversationThread> conversationThreads, PostBoxResponse postBoxResponse) {
        for (ConversationThread conversationThread : conversationThreads) {
            postBoxResponse.addItem(createSinglePostBoxItem(email, conversationThread));
        }
    }


    private PostBoxListItemResponse createSinglePostBoxItem(String email, ConversationThread conversationThread) {
        ConversationBlock conversationBlock = conversationBlockRepository.byConversationId(conversationThread.getConversationId());

        return new PostBoxListItemResponse(email, conversationThread, conversationBlock);
    }
}
