package com.ecg.replyts.core.runtime.persistence;

import org.joda.time.DateTime;

public final class TimestampIndexValue {

    private TimestampIndexValue() {
    }


    public static long timestampInMinutes(long millis) {
        return millis / (1000 * 60);
    }

    public static long timestampInMinutes(DateTime time) {
        return timestampInMinutes(time.getMillis());
    }

}
