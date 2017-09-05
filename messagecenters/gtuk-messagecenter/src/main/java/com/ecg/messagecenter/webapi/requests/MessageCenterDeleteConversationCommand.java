package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.util.Optional;

public class MessageCenterDeleteConversationCommand implements TypedCommand {

    public static final String MAPPING = "/postboxes/{email}/conversations/{conversationId}";

    private final String email;
    private String conversationId;

    public MessageCenterDeleteConversationCommand(String email, String conversationId) {
        this.email = email;
        this.conversationId = conversationId;
    }

    @Override
    public Method method() {
        return Method.DELETE;
    }

    @Override
    public String url() {
        return "/postboxes/" + email + "/conversations/" + conversationId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }
}
