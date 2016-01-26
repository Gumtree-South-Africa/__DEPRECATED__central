package com.ecg.replyts.core.api.processing;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Will be thrown if total processing time of a message are exceeded.
 *
 * Use RuntimeException to prevent breaking legacy code based on interface {@link com.ecg.replyts.core.api.pluginconfiguration.filter.Filter}.
 */
public class ProcessingTimeExceededException extends RuntimeException {

    /**
     * @param maxAllowedProcessingTimeMs The max allowed configured processing time
     * @param processingTimeMs The actual needed time after stop processing
     */
    ProcessingTimeExceededException(long maxAllowedProcessingTimeMs, long processingTimeMs) {
        super(format("Max processing time for message exceeded. Max allowed %d s, stopped after %d s",
                MILLISECONDS.toSeconds(maxAllowedProcessingTimeMs),
                MILLISECONDS.toSeconds(processingTimeMs)));
    }

}
