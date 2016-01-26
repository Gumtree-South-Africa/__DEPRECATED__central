package com.ecg.replyts.app.preprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PreProcessorManager {

    private static final transient Logger LOG = LoggerFactory.getLogger(PreProcessorManager.class);

    private final PreProcessor conversationFinder;
    private final PreProcessor automatedMailRemover;
    private final PreProcessor discardMailOnClosedStatePreProcessor;

    @Autowired
    public PreProcessorManager(
            @Qualifier("conversationFinder") PreProcessor conversationFinder,
            @Qualifier("automatedMailRemover") PreProcessor automatedMailRemover,
            @Qualifier("discardMailOnClosedStatePreProcessor")  PreProcessor discardMailOnClosedStatePreProcessor) {
        this.conversationFinder = conversationFinder;
        this.automatedMailRemover = automatedMailRemover;
        this.discardMailOnClosedStatePreProcessor= discardMailOnClosedStatePreProcessor;
    }

    public void preProcess(MessageProcessingContext context) {
        LOG.debug("Finding conversation on message {}", context.getMessageId());
        conversationFinder.preProcess(context);
        if (context.isTerminated()) {
            LOG.debug("Could not assign conversation for message {}, ended in {}", context.getMessageId(), context.getTermination().getEndState());
            return;
        }
        LOG.debug("Checking if message {} is automated reply", context.getMessageId());
        automatedMailRemover.preProcess(context);
        if (context.isTerminated()) {
            LOG.debug("Message {} is automated reply: {}", context.getMessageId(), context.getTermination().getReason());
            return;
        }

        LOG.debug("Checking if conversation {} is closed", context.getConversation().getId());
        discardMailOnClosedStatePreProcessor.preProcess(context);
        if (context.isTerminated()) {
            LOG.debug("Conversation {} is closed reply: {}", context.getConversation().getId(), context.getTermination().getReason());
            return;
        }
    }

}
