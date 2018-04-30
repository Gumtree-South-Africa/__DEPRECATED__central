package com.ecg.messagecenter.core.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.util.Optional;

public class MessageCenterDeletePostBoxConversationCommandNew implements TypedCommand {

    public static final String MAPPING = "/postboxes/{email}/conversations";

    private final String email;

    public MessageCenterDeletePostBoxConversationCommandNew(String email) {
        this.email = email;
    }

    @Override
    public Method method() {
        return Method.DELETE;
    }

    @Override
    public String url() {
        return "/postboxes/" + email + "/conversations";
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }

}
