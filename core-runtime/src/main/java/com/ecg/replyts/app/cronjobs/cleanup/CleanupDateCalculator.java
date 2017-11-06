package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import java.time.LocalTime;
import java.util.TimeZone;

import static org.joda.time.DateTime.now;

@Component
@ConditionalOnExpression("('${replyts2.cleanup.conversation.enabled:false}' == '${region}' || '${replyts2.cleanup.postboxes.enabled:false}' == '${region}') && " +
                         "('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid'))")
public class CleanupDateCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupDateCalculator.class);

    private static final DateTimeFieldType ROUNDING_TO = DateTimeFieldType.hourOfDay();

    private final CronJobClockRepository cronJobClockRepository;
    private final TimeZone timeZone;
    private final LocalTime quietTimeStart;
    private final LocalTime quietTimeEnd;

    @Autowired
    public CleanupDateCalculator(CronJobClockRepository cronJobClockRepository,
            @Value("${replyts.tenant.timezone:Europe/Amsterdam}") String timeZone,
            @Value("${replyts.cleanup.quietTime.start:16:00}") String quietTimeStart,
            @Value("${replyts.cleanup.quietTime.end:23:00}") String quietTimeEnd) {
        this.cronJobClockRepository = cronJobClockRepository;
        this.timeZone = TimeZone.getTimeZone(timeZone);
        this.quietTimeStart = LocalTime.parse(quietTimeStart);
        this.quietTimeEnd = LocalTime.parse(quietTimeEnd);
    }

    @PostConstruct
    public void postConstruct()
    {
        LOG.info("Cleanup quiet time: {}-{} {}", quietTimeStart, quietTimeEnd, timeZone.getID());
    }

    /**
     * Gets the cleanup date based on the maxAgeDays and the lastProcessedDate for the provided job. We round to hourOfDay, taking into account
     * the local timezone as specified by <code>replyts2.tenant.timezone</code> and <code>replyts2.cleanup.quietTime</code>.
     *
     * @return next rounded cleanup date or null in case nothing should be cleaned up
     */
    @Nullable
    public DateTime getCleanupDate(int maxAgeDays, String jobName) {
        LocalTime now = LocalTime.now(timeZone.toZoneId());
        if (now.isAfter(quietTimeStart) && now.isBefore(quietTimeEnd)) {
            return null;
        }

        Period incrementPeriod = new Period().withField(ROUNDING_TO.getDurationType(), 1);

        // Instant in time past which we shouldn't clean any further

        DateTime lastCleanupDate = now().minusDays(maxAgeDays);
        DateTime roundedLastCleanupDate = lastCleanupDate.property(ROUNDING_TO).roundFloorCopy().toDateTime();

        // Instant in time which we left off at (was cleaned last)

        DateTime lastProcessedDate = cronJobClockRepository.getLastProcessedDate(jobName);

        // We round this date as well to account for old (not yet rounded) data

        DateTime roundedLastProcessedDate = lastProcessedDate != null ? lastProcessedDate.property(ROUNDING_TO).roundFloorCopy().toDateTime() : null;

        if (lastProcessedDate != null && roundedLastProcessedDate.isBefore(roundedLastCleanupDate)) {
            return roundedLastProcessedDate.withPeriodAdded(incrementPeriod, 1);
        } else if (lastProcessedDate != null) {
            LOG.info("Cleanup: All data was already deleted for job {} on date {} (cleaning period set to {})", jobName, lastProcessedDate, ROUNDING_TO.getDurationType());

            return null;
        } else {
            return roundedLastCleanupDate;
        }
    }
}
