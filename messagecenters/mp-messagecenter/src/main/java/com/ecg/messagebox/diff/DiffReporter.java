package com.ecg.messagebox.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
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