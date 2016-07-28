package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

public class DeleteConversationsCommand implements TypedCommand {

    public static final String MAPPING = "/postboxes/{userId}/conversations";

    private final String userId;

    public DeleteConversationsCommand(String userId) {
        this.userId = userId;
    }

    @Override
    public Method method() {
        return Method.DELETE;
    }

    @Override
    public String url() {
        return "/postboxes/" + userId + "/conversations";
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}