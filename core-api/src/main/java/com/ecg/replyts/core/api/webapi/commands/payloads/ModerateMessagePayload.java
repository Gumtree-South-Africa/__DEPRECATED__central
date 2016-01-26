package com.ecg.replyts.core.api.webapi.commands.payloads;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;

/**
 * JSON Payload for moderate message command
 */
public class ModerateMessagePayload {

    private MessageState currentMessageState;

    private ModerationResultState newMessageState;

    private String editor;

    public ModerationResultState getNewMessageState() {
        return newMessageState;
    }

    public void setNewMessageState(ModerationResultState newMessageState) {
        this.newMessageState = newMessageState;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public MessageState getCurrentMessageState() {
        return currentMessageState;
    }

    public void setCurrentMessageState(MessageState currentMessageState) {
        this.currentMessageState = currentMessageState;
    }
}
