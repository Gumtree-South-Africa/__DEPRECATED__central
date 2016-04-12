package com.ecg.replyts.integration.test.postprocessor;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

/**
 * Leaks the Message and Conversation ID into the outbound mail. Vital for Automation testing to figure out which id a mail has been assigned to.
 */
public class IdLeakingPostProcessor implements PostProcessor {

    public static final String X_RTS_CONVID = "X-RTS-CONVID";
    public static final String X_RTS_MSGID = "X-RTS-MSGID";

    @Override
    public void postProcess(MessageProcessingContext context) {
        try {
            context.getOutgoingMail().addHeader(X_RTS_CONVID, context.getConversation().getId());
            context.getOutgoingMail().addHeader(X_RTS_MSGID, context.getMessage().getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int getOrder() {
        return 0;
    }
}
