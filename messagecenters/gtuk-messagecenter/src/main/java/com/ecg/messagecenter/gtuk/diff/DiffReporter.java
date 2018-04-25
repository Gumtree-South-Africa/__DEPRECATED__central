package com.ecg.messagecenter.gtuk.diff;

import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class DiffReporter {

    public static final Marker DIFF_MARKER = Markers.append("component", "diffing");
    private static final Logger LOGGER = LoggerFactory.getLogger("diffLogger");
    private static final Logger NEW_LOGGER = LoggerFactory.getLogger("newDiffLogger");

    public static void report(String errorMessage, boolean useNewLogger) {
        if (useNewLogger) {
            NEW_LOGGER.error(DIFF_MARKER, errorMessage);
        } else {
            LOGGER.error(DIFF_MARKER, errorMessage);
        }
    }
}