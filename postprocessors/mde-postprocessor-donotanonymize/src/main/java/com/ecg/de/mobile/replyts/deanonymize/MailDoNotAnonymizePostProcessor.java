package com.ecg.de.mobile.replyts.deanonymize;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

/**
 * User: beckart
 */
class MailDoNotAnonymizePostProcessor implements PostProcessor {

    private final String password;

    MailDoNotAnonymizePostProcessor(String password) {
        this.password = password;
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        new MailDoNotAnonymizeHandler(messageProcessingContext, password).handle();
    }
}
