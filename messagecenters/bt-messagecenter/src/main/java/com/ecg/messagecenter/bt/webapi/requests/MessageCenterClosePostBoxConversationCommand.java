package com.ecg.messagecenter.bt.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.util.Optional;

public class MessageCenterClosePostBoxConversationCommand implements TypedCommand {
    public static final String MAPPING = "/postboxes/{email}/conversations/{conversationId}/state/CLOSED";

    private final String email;
    private String conversationId;

    public MessageCenterClosePostBoxConversationCommand(String email, String conversationId) {
        this.email = email;
        this.conversationId = conversationId;
    }

    @Override
    public Method method() {
        return Method.PUT;
    }

    @Override
    public String url() {
        return "/postboxes/" + email + "/conversations/" + conversationId + "/CLOSED";
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }
}
