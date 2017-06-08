package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jaludden on 20/11/15.
 */
public class MessageCenterStartConversationCommand implements TypedCommand {

    public static final String MAPPING = "/chat/{email}/conversations";

    private StartConversationContentPayload payload;

    public MessageCenterStartConversationCommand(StartConversationContentPayload payload) {
        checkNotNull(payload);
        this.payload = payload;
    }

    @Override public Method method() {
        return Method.POST;
    }

    @Override public String url() {
        return MAPPING;
    }

    @Override public Optional<String> jsonPayload() {
        try {
            return Optional.of(JsonObjects.getObjectMapper().writeValueAsString(payload));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
