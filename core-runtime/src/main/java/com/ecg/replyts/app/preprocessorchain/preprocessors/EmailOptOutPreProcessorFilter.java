package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public interface EmailOptOutPreProcessorFilter {

    boolean filter(MessageProcessingContext context);
}