package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.util.Clock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeframeTest {

    private Clock clock = mock(Clock.class);

    private Timeframe timeframe;

    @Before
    public void setUp() {
        timeframe = new Timeframe(10, 20, 2, clock);
    }

    @Test
    public void doesNotFireOutsideOfRange() {
        when(clock.now()).thenReturn(DateTime.parse("2013-01-01T09:20:00").toDate());
        assertFalse(timeframe.operateNow());
    }

    @Test
    public void doesNotFireInWorkingHoursButBeforeRetentionTimeIsOver() {
        when(clock.now()).thenReturn(DateTime.parse("2013-01-01T11:59:00").toDate());
        assertFalse(timeframe.operateNow());
    }

    @Test
    public void doesFireAfterWorkingHoursStartAndRetentionTimeIsOver() {
        when(clock.now()).thenReturn(DateTime.parse("2013-01-01T12:00:00").toDate());
        assertTrue(timeframe.operateNow());
    }

    @Test
    public void firesUntilEndOfWorkingHours() {
        when(clock.now()).thenReturn(DateTime.parse("2013-01-01T19:59:59").toDate());
        assertTrue(timeframe.operateNow());
    }

    @Test
    public void doesNotFireAfterWorkingHour() {
        when(clock.now()).thenReturn(DateTime.parse("2013-01-01T20:00:00").toDate());
        assertFalse(timeframe.operateNow());
    }
}
