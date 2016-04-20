package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.joda.time.DateTime.now;

/**
 * Determines the clean up date for the jobs.
 */
public class CleanupDateCalculator {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupDateCalculator.class);

    private CronJobClockRepository cronJobClockRepository;

    public CleanupDateCalculator(CronJobClockRepository cronJobClockRepository) {
        this.cronJobClockRepository = cronJobClockRepository;
    }

    /**
     * Gets the cleanup date based on the maxAgeDays and the lastProcessedDate for the
     * provided job
     * @param maxAgeDays the max age days
     * @param jobName the job name to get the last processed date
     * @return the cleanup date or null in case nothing should be cleaned up
     */
    public DateTime getCleanupDate(int maxAgeDays, String jobName) {
        DateTime dateToBeProcessed = now().minusDays(maxAgeDays);

        DateTime lastProcessedDate = cronJobClockRepository.getLastProcessedDate(jobName);

        // it is necessary to compare dates without a time part
        if (lastProcessedDate != null && lastProcessedDate.toLocalDate().isBefore(dateToBeProcessed.toLocalDate())) {
            return lastProcessedDate.plusDays(1);
        } else if (lastProcessedDate != null) {
            LOG.info("Cleanup: All data was already deleted for the job '{}' for the date '{}'", jobName, dateToBeProcessed);
            return null;
        }
        return dateToBeProcessed;
    }
}
