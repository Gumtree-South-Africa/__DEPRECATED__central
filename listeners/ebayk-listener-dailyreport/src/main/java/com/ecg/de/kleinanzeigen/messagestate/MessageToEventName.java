package com.ecg.de.kleinanzeigen.messagestate;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.util.JsonObjects.Builder;

/** Determines the log event based on the given message */
class MessageToEventName {

    public String jsonLogEntry(Message message) {
        Builder bdr = JsonObjects.builder().attr("event", describeState(message));
        bdr.attr("agent", message.getLastEditor().orElse(null));
        return bdr.toJson();
    }

    String describeState(Message message) {
        if(messageWasModerated(message)) {
            return message.getState().name();
        } else {
            String fromState = message.getFilterResultState().name();
            String toState = message.getHumanResultState() == ModerationResultState.GOOD ?
                    MessageState.SENT.name() : MessageState.BLOCKED.name();
            return "FROM_"+fromState+"_TO_"+toState;
        }
    }

    private boolean messageWasModerated(Message message) {
        return message.getHumanResultState().equals(ModerationResultState.UNCHECKED);
    }
}
