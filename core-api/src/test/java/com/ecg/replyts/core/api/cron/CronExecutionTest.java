package com.ecg.replyts.core.api.cron;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class CronExecutionTest {

    @Test
    public void sameObjectsAreEqual() {
        DateTime now = now();
        assertEquals(new CronExecution("foojob", now, "localhost"), new CronExecution("foojob", now, "localhost"));
    }

    @Test
    public void differentObjectsAreNotEqual() {
        DateTime now = now();
        assertNotSame(new CronExecution("foojob", now, "localhost"), new CronExecution("foojob", now, "otherhost"));
    }
}
