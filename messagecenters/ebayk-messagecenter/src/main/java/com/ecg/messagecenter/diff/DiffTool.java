package com.ecg.messagecenter.diff;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "webapi.diff.ek.enabled", havingValue = "true")
public class DiffTool {

    private PostBoxResponseDiff postBoxDiff;
    private ConversationResponseDiff conversationDiff;
    private final ConversationDeleteResponseDiff conversationDeleteDiff;

    @Autowired
    public DiffTool(PostBoxResponseDiff postBoxDiff, ConversationResponseDiff conversationDiff, ConversationDeleteResponseDiff conversationDeleteDiff) {
        this.postBoxDiff = postBoxDiff;
        this.conversationDiff = conversationDiff;
        this.conversationDeleteDiff = conversationDeleteDiff;
    }

    void postBoxResponseDiff(String userId, PostBox newValue, PostBoxDiff oldValue) {
        postBoxDiff.diff(userId, newValue, oldValue);
    }

    void conversationResponseDiff(String userId, String conversationId, Optional<ConversationThread> newValue, Optional<PostBoxSingleConversationThreadResponse> oldValue) {
        conversationDiff.diff(userId, conversationId, newValue, oldValue);
    }

    void conversationDeleteResponseDiff(String userId, String conversationId, Optional<ConversationThread> newValue, Optional<ConversationRts> oldValue) {
        conversationDeleteDiff.diff(userId, conversationId, newValue, oldValue);
    }
}