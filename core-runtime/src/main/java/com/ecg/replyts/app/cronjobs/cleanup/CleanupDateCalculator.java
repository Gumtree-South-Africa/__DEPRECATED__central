package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static org.joda.time.DateTime.now;

@Component
@ConditionalOnExpression("#{('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid')) && '${replyts2.cleanup.conversation.enabled}' == 'true'}")
public class CleanupDateCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupDateCalculator.class);

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    public DateTime getCleanupDate(int maxAgeDays, String jobName) {
        return getCleanupDate(maxAgeDays, jobName, DateTimeFieldType.dayOfMonth());
    }

    /**
     * Gets the cleanup date based on the maxAgeDays and the lastProcessedDate for the provided job. We round both
     * to the given rounding field (e.g. dayOfMonth or hourOfDay).
     *
     * @param maxAgeDays
     * @param jobName
     * @param roundingTo
     * @return next rounded cleanup date (or null in case nothing should be cleaned up)
     */
    public DateTime getCleanupDate(int maxAgeDays, String jobName, DateTimeFieldType roundingTo) {
        Period incrementPeriod = new Period().withField(roundingTo.getDurationType(), 1);

        // Instant in time past which we shouldn't clean any further

        DateTime lastCleanupDate = now().minusDays(maxAgeDays);
        DateTime roundedLastCleanupDate = lastCleanupDate.property(roundingTo).roundFloorCopy().toDateTime();

        // Instant in time which we left off at (was cleaned last)

        DateTime lastProcessedDate = cronJobClockRepository.getLastProcessedDate(jobName);

        // We round this date as well to account for old (not yet rounded) data

        DateTime roundedLastProcessedDate = lastProcessedDate != null ? lastProcessedDate.property(roundingTo).roundFloorCopy().toDateTime() : null;

        if (lastProcessedDate != null && roundedLastProcessedDate.isBefore(roundedLastCleanupDate)) {
            return roundedLastProcessedDate.withPeriodAdded(incrementPeriod, 1);
        } else if (lastProcessedDate != null) {
            LOG.info("Cleanup: All data was already deleted for job {} on date {} (cleaning period set to {})", jobName, lastProcessedDate, roundingTo.getDurationType());

            return null;
        } else {
            return roundedLastCleanupDate;
        }
    }
}
