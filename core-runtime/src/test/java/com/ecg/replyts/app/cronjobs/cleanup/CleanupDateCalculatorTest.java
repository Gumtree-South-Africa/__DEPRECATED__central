package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CleanupDateCalculatorTest {

    private static final String TEST_JOB_NAME = "test_job";
    private static final int MAX_AGE_DAYS = 10;

    private CronJobClockRepository cronJobClockRepository;

    private CleanupDateCalculator testObject;

    @Before
    public void init() {
        cronJobClockRepository = mock(CronJobClockRepository.class);
        testObject = new CleanupDateCalculator(cronJobClockRepository);
    }

    @Test
    public void getCleanupDateWhenNoLastProcessedDate() {
        when(cronJobClockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(null);

        DateTime cleanupDate = testObject.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);

        assertEquals(dateToBeProcessed.toLocalDate(), cleanupDate.toLocalDate());
    }

    @Test
    public void getCleanupDateWhenLastProcessedDateIsBeforeDateToBeProcess() {
        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate= dateToBeProcessed.minusDays(2);

        when(cronJobClockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = testObject.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertEquals(lastProcessedDate.plusDays(1).toLocalDate(), cleanupDate.toLocalDate());
    }

    @Test
    public void getCleanupDateWhenLastProcessedDateIsAfterDateToBeProcess() {
        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate= dateToBeProcessed.plusDays(2);

        when(cronJobClockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = testObject.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertNull(cleanupDate);
    }
}
