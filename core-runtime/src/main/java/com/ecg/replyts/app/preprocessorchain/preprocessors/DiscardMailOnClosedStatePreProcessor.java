package com.ecg.replyts.app.preprocessorchain.preprocessors;


import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @author huttar
 */
@Component("discardMailOnClosedStatePreProcessor")
public class DiscardMailOnClosedStatePreProcessor implements PreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DiscardMailOnClosedStatePreProcessor.class);

    @Override
    public void preProcess(MessageProcessingContext context) {
        LOG.debug("Checking if conversation {} is closed", context.getConversation().getId());

        if (context.getConversation().getState() == ConversationState.CLOSED) {
            context.terminateProcessing(MessageState.DISCARDED,this, "Conversation is closed");
            LOG.debug("Conversation {} is closed reply: {}", context.getConversation().getId(), context.getTermination().getReason());
        }
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
