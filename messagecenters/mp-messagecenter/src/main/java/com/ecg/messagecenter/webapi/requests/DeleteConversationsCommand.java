package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

public class DeleteConversationsCommand implements TypedCommand {

    public static final String MAPPING = "/postboxes/{postBoxId}/conversations";

    private final String postBoxId;

    public DeleteConversationsCommand(String postBoxId) {
        this.postBoxId = postBoxId;
    }

    @Override
    public Method method() {
        return Method.DELETE;
    }

    @Override
    public String url() {
        return "/postboxes/" + postBoxId + "/conversations";
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}
