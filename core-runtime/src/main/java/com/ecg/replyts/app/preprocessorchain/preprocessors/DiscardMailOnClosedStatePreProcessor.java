package com.ecg.replyts.app.preprocessorchain.preprocessors;


import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.stereotype.Component;


/**
 * @author huttar
 */
@Component("discardMailOnClosedStatePreProcessor")
public class DiscardMailOnClosedStatePreProcessor implements PreProcessor {

    @Override
    public void preProcess(MessageProcessingContext context) {
        if (context.getConversation().getState() == ConversationState.CLOSED) {
            context.terminateProcessing(MessageState.DISCARDED,this, "Conversation is closed");
        }
    }


}
