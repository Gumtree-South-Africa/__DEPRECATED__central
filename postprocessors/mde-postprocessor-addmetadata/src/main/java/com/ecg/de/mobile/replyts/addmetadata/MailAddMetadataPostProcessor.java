package com.ecg.de.mobile.replyts.addmetadata;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

/**
 * User: beckart
 */
public class MailAddMetadataPostProcessor implements PostProcessor {

    private final Integer orderNumber;

    MailAddMetadataPostProcessor(Integer orderNumber) {
        if(orderNumber <= 200) {
            throw new RuntimeException("Order has to higher than 200 to performed after mail cloaking! ");
        }
        this.orderNumber = orderNumber;
    }

    @Override
    public int getOrder() {
        return orderNumber;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        new MailAddMetadataHandler(messageProcessingContext).handle();
    }
}
