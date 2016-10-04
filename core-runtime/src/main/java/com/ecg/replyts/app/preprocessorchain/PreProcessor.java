package com.ecg.replyts.app.preprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

/**
 *
 */

public interface PreProcessor {

    void preProcess(MessageProcessingContext context);

}