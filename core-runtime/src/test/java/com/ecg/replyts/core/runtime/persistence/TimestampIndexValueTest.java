package com.ecg.replyts.core.runtime.persistence;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimestampIndexValueTest {

    @Test
    public void calculatesCorrectTimestamp() {

        assertEquals(23143560l, TimestampIndexValue.timestampInMinutes(DateTime.parse("2014-01-01T23:00:00.000+01:00")));

    }
}
