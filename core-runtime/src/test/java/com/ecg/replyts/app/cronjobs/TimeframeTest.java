package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.util.Clock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TimeframeTest.TestContext.class)
@TestPropertySource(properties = {
  "cronjob.sendHeld.csWorkingHoursStart = 10",
  "cronjob.sendHeld.csWorkingHoursEnd = 20",
  "cronjob.sendHeld.retentionTimeHours = 2"
})
public class TimeframeTest {
    @Autowired
    private Timeframe timeframe;

    @Mock
    private Clock clock;

    @Before
    public void init() {
        ReflectionTestUtils.setField(timeframe, "clock", clock);
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

    @Configuration
    @Import(Timeframe.class)
    static class TestContext {
        @MockBean
        private Clock clock;
    }
}
