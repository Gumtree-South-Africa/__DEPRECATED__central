package com.ecg.replyts.app.preprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.core.Ordered;

public interface PreProcessor extends Ordered {

    void preProcess(MessageProcessingContext context);

}