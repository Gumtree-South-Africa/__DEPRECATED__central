package com.ecg.replyts.app.postprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public interface EmailPostProcessor extends PostProcessor {
    @Override
    default boolean isApplicable(MessageProcessingContext context) {
        return context.getOutgoingMail() != null;
    }
}
