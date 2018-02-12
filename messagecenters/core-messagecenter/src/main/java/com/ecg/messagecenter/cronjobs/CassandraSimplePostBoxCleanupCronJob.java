package com.ecg.messagecenter.cronjobs;

import com.ecg.messagecenter.persistence.simple.CassandraSimplePostBoxRepository;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static org.joda.time.DateTime.now;

@Component
@ConditionalOnExpression("#{'${replyts2.cleanup.postboxes.enabled}' == '${region}' && ('${persistence.strategy}'.startsWith('cassandra') || '${persistence.strategy}'.startsWith('hybrid'))}")
public class CassandraSimplePostBoxCleanupCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraSimplePostBoxCleanupCronJob.class);

    protected static final String CLEANUP_CONVERSATION_JOB_NAME = "cleanupPostboxesJob";

    @Autowired
    private CassandraSimplePostBoxRepository postBoxRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Value("${replyts.cleanup.postboxes.schedule.expression:0 0 * * * ? *}")
    private String cronJobExpression;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    @Value("${comaas.cleanup.postbox.skipFailed:false}")
    private boolean skipFailed;

    @Override
    public void execute() throws Exception {
        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(maxConversationAgeDays, CLEANUP_CONVERSATION_JOB_NAME);

        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting postbox conversations for the date {}", cleanupDate);

        if (postBoxRepository.cleanup(cleanupDate)) {
            cronJobClockRepository.set(CLEANUP_CONVERSATION_JOB_NAME, now(), cleanupDate);
            LOG.info("Cleanup: Finished deleting postbox conversations");
        } else if (skipFailed) {
            cronJobClockRepository.set(CLEANUP_CONVERSATION_JOB_NAME, now(), cleanupDate);
            LOG.info("Cleanup: Deleting postbox conversations failed, skipping to next time frame");
        } else {
            LOG.warn("Cleanup: Deleting postbox conversations failed");
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }
}
