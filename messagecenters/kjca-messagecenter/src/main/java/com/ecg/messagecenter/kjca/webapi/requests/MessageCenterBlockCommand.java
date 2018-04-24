package com.ecg.messagecenter.kjca.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.util.Optional;

public class MessageCenterBlockCommand implements TypedCommand {

    public static final String MAPPING = "/postboxes/{email}/conversations/{conversationId}/block";

    private final String email;
    private String conversationId;

    public MessageCenterBlockCommand(String email, String conversationId) {
        this.email = email;
        this.conversationId = conversationId;
    }

    @Override
    public Method method() {
        return Method.POST; // and DELETE
    }

    @Override
    public String url() {
        return "/postboxes/" + email + "/conversations/" + conversationId + "/block";
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }
}
