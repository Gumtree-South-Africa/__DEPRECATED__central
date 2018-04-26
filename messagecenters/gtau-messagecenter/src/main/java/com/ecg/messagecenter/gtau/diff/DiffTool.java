package com.ecg.messagecenter.gtau.diff;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.gtau.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.sync.PostBoxDiff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "webapi.diff.au.enabled", havingValue = "true")
public class DiffTool {

    private PostBoxResponseDiff postBoxDiff;
    private ConversationResponseDiff conversationDiff;

    @Autowired
    public DiffTool(PostBoxResponseDiff postBoxDiff, ConversationResponseDiff conversationDiff) {
        this.postBoxDiff = postBoxDiff;
        this.conversationDiff = conversationDiff;
    }

    void postBoxResponseDiff(String userId, PostBox newValue, PostBoxDiff oldValue) {
        postBoxDiff.diff(userId, newValue, oldValue);
    }

    void conversationResponseDiff(String userId, String conversationId, Optional<ConversationThread> newValue, Optional<PostBoxSingleConversationThreadResponse> oldValue) {
        conversationDiff.diff(userId, conversationId, newValue, oldValue);
    }
}