package com.ecg.messagecenter.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class DiffReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("diffLogger");
    private static final Logger NEW_LOGGER = LoggerFactory.getLogger("newDiffLogger");

    public void report(String errorMessage, boolean useNewLogger) {
        if (useNewLogger) {
            NEW_LOGGER.error(errorMessage);
        } else {
            LOGGER.error(errorMessage);
        }
    }
}