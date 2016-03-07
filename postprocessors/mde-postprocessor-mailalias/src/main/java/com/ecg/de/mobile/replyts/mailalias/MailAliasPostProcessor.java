package com.ecg.de.mobile.replyts.mailalias;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: beckart
 */
class MailAliasPostProcessor implements PostProcessor {

    MailAliasPostProcessor() {

    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        new MailAliasHandler(messageProcessingContext).handle();
    }
}
