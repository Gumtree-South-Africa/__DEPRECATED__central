package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

public class PostBoxConversationCommand implements TypedCommand {

    public static final String MAPPING = "/postboxes/{userId}/conversations/{conversationId}";

    private final String userId;
    private String conversationId;

    public PostBoxConversationCommand(String userId, String conversationId) {
        this.userId = userId;
        this.conversationId = conversationId;
    }

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/postboxes/" + userId + "/conversations/" + conversationId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}
