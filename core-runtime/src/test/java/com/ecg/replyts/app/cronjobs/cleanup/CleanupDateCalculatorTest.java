package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CleanupDateCalculatorTest.TestContext.class)
@TestPropertySource(properties = {
  "persistence.strategy = cassandra",
  "cronjob.cleanup.conversation.enabled = false",
  "region = localhost"
})
public class CleanupDateCalculatorTest {
    private static final String TEST_JOB_NAME = "test_job";

    private static final int MAX_AGE_DAYS = 10;

    @Autowired
    private CronJobClockRepository clockRepository;

    @Test
    public void getCleanupDateWhenNoLastProcessedDate() {
        CleanupDateCalculator cleanupDateCalculator = new CleanupDateCalculator(clockRepository, "Europe/Amsterdam", "00:00", "00:00");

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(null);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);

        assertEquals(dateToBeProcessed.toLocalDate(), cleanupDate.toLocalDate());
    }

    @Test
    public void getCleanupDateWhenLastProcessedDateIsBeforeDateToBeProcess() {
        CleanupDateCalculator cleanupDateCalculator = new CleanupDateCalculator(clockRepository, "Europe/Amsterdam", "00:00", "00:00");

        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate= dateToBeProcessed.minusDays(2);

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertEquals(lastProcessedDate.plusHours(1).toLocalDate(), cleanupDate.toLocalDate());
    }

    @Test
    public void getCleanupDateWhenLastProcessedDateIsAfterDateToBeProcess() {
        CleanupDateCalculator cleanupDateCalculator = new CleanupDateCalculator(clockRepository, "Europe/Amsterdam", "00:00", "00:00");

        DateTime dateToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate= dateToBeProcessed.plusDays(2);

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertNull(cleanupDate);
    }

    @Test
    public void getCleanupDateWhenIncrementIsAnHour() {
        CleanupDateCalculator cleanupDateCalculator = new CleanupDateCalculator(clockRepository, "Europe/Amsterdam", "00:00", "00:00");

        DateTime lastMomentToBeProcessed = now().minusDays(MAX_AGE_DAYS);
        DateTime lastProcessedDate = lastMomentToBeProcessed.minusHours(1);

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(lastProcessedDate);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertEquals("Cleanup date is equal to the last cleanup hour", lastMomentToBeProcessed.hourOfDay().roundFloorCopy().toDateTime(), cleanupDate);
    }

    @Test
    public void getLastProcessedDate_whenInsideQuietZone_shouldReturnNull() {
        LocalTime now = LocalTime.now(DateTimeZone.forID("Europe/Amsterdam"));

        CleanupDateCalculator cleanupDateCalculator = new CleanupDateCalculator(clockRepository, "Europe/Amsterdam",
                now.minusMinutes(2).toString(), now.plusMinutes(2).toString());

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(null);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertThat(cleanupDate).isNull();
    }

    @Test
    public void getLastProcessedDate_whenOutsideQuietZone_shouldReturnNull() {
        LocalTime now = LocalTime.now(DateTimeZone.forID("Europe/Amsterdam"));

        CleanupDateCalculator cleanupDateCalculator = new CleanupDateCalculator(clockRepository, "Europe/Amsterdam",
                now.plusMinutes(2).toString(), now.minusMinutes(2).toString());

        when(clockRepository.getLastProcessedDate(TEST_JOB_NAME)).thenReturn(null);

        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, TEST_JOB_NAME);

        assertThat(cleanupDate).isNotNull();
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
