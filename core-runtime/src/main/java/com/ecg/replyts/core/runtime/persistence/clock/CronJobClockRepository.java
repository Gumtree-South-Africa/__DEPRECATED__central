package com.ecg.replyts.core.runtime.persistence.clock;

import org.joda.time.DateTime;

/**
 * Contains methods related to setting or retrieving the cron job clock.
 */
public interface CronJobClockRepository {

    /**
     * Sets the last run date and processed date for the cron job.
     * @param cronJobName the cron job name
     * @param lastRunDate the last run date
     * @param lastProcessedDate the last processed date
     */
    void set(String cronJobName, DateTime lastRunDate, DateTime lastProcessedDate);

    /**
     * Gets the last processed date.
     * @param cronJobName the cron job name
     * @return the last processed date or null
     */
    DateTime getLastProcessedDate(String cronJobName);
}
