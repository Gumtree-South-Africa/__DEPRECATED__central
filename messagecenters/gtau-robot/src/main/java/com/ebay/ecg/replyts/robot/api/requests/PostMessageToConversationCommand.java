package com.ebay.ecg.replyts.robot.api.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.util.Optional;

/**
 * @author mdarapour
 */
public class PostMessageToConversationCommand implements TypedCommand {

    public static final String MAPPING = "/conversations/{conversationId}";

    private String conversationId;

    public PostMessageToConversationCommand(String conversationId) {
        this.conversationId = conversationId;
    }

    @Override
    public Method method() {
        return Method.POST;
    }

    @Override
    public String url() {
        return "/conversations/" + conversationId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }
}
