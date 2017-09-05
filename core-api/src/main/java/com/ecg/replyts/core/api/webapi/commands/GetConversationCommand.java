package com.ecg.replyts.core.api.webapi.commands;


import com.ecg.replyts.core.api.webapi.Method;

import java.util.Optional;

public class GetConversationCommand implements TypedCommand {

    public static final String MAPPING = "/conversation/{conversationId}";

    private final String conversationId;

    public GetConversationCommand(String conversationId) {
        this.conversationId = conversationId;
    }


    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/conversation/" + conversationId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }

}
