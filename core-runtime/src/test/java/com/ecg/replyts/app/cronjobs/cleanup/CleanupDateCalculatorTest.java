package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CleanupDateCalculatorTest.TestContext.class)
@TestPropertySource(properties = {
  "persistence.strategy = cassandra",
  "replyts2.cleanup.conversation.enabled = localhost",
  "region = localhost"
})
public class CleanupDateCalculatorTest {
    private static final String TEST_JOB_NAME = "test_job";

    private static final int MAX_AGE_DAYS = 10;

    @Autowired
    private CronJobClockRepository clockRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Test
    public void getCleanupDateWhenNoLastProcessedDate() {
        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(null);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);

        assertEquals(dateToBeProcessed.toLocalDate(), cleanupDate.toLocalDate());
    }

    @Test
    public void getCleanupDateWhenLastProcessedDateIsBeforeDateToBeProcess() {
        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate= dateToBeProcessed.minusDays(2);

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertEquals(lastProcessedDate.plusHours(1).toLocalDate(), cleanupDate.toLocalDate());
    }

    @Test
    public void getCleanupDateWhenLastProcessedDateIsAfterDateToBeProcess() {
        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate= dateToBeProcessed.plusDays(2);

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertNull(cleanupDate);
    }

    @Test
    public void getCleanupDateWhenIncrementIsAnHour() {
        DateTime lastMomentToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate = lastMomentToBeProcessed.minusHours(1);

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME, DateTimeFieldType.hourOfDay());

        assertEquals("Cleanup date is equal to the last cleanup hour", lastMomentToBeProcessed.hourOfDay().roundFloorCopy().toDateTime(), cleanupDate);
    }

    @Configuration
    @Import(CleanupDateCalculator.class)
    static class TestContext {
        @Bean
        public CronJobClockRepository clockRepository() {
            return mock(CronJobClockRepository.class);
        }
    }
}
