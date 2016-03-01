package com.ecg.de.kleinanzeigen.negotiations;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.springframework.beans.factory.annotation.Autowired;

public class NegotiationDelayListener implements MessageProcessedListener {

    private final Caller caller;

    @Autowired
    public NegotiationDelayListener(Caller caller) {
        this.caller = caller;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (message.getState() != MessageState.SENT && containsOffer(message)) {
            caller.execute(Caller.NegotationState.DELAYED__FILTER_DELAYED, conversation, message);
        }
    }

    boolean containsOffer(Message msg) {
        return msg.getHeaders().containsKey("X-Offerid");
    }

}
