package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessageCenterSendMessageCommand implements TypedCommand {

    public static final String MAPPING = "/chat/{email}/conversations/{conversationId}";

    private ConversationContentPayload conversationContent;

    public MessageCenterSendMessageCommand(ConversationContentPayload conversationContent) {
        checkNotNull(conversationContent);
        this.conversationContent = conversationContent;
    }

    @Override
    public Method method() {
        return Method.POST;
    }

    @Override
    public String url() {
        return MAPPING;
    }

    @Override
    public Optional<String> jsonPayload() {
        try {
            return Optional.of(JsonObjects.getObjectMapper().writeValueAsString(conversationContent));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
