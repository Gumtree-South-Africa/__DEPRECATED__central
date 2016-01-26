package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.payloads.ModerateMessagePayload;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.io.IOException;

import static com.ecg.replyts.core.api.util.JsonObjects.getObjectMapper;
import static com.google.common.base.Optional.of;

public class CloseConversationCommand implements TypedCommand {


    public static final String MAPPING = "/message/{conversationId}/{messageId}/state";

    private final String messageId;


    private final String conversationId;

    private ModerateMessagePayload payload = new ModerateMessagePayload();


    public CloseConversationCommand(String conversationId, String messageId, ModerationResultState resultState) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        Preconditions.checkArgument(resultState == ModerationResultState.GOOD || resultState == ModerationResultState.BAD);
        payload.setNewMessageState(resultState);
    }


    @Override
    public Method method() {
        return Method.POST;
    }

    @Override
    public String url() {
        return "/message/" + conversationId + "/" + messageId + "/state";
    }

    @Override
    public Optional<String> jsonPayload() {
        try {
            return of(getObjectMapper().writeValueAsString(payload));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
