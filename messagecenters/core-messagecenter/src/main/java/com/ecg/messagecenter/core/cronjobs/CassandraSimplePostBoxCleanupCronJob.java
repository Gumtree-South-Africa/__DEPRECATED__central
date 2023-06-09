package com.ecg.messagecenter.core.cronjobs;

import com.ecg.messagecenter.core.persistence.simple.CassandraSimpleMessageCenterRepository;
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
@ConditionalOnExpression("#{'${cronjob.cleanup.postboxes.enabled:false}' == 'true' && '${active.dc}' != '${region}'}")
public class CassandraSimplePostBoxCleanupCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraSimplePostBoxCleanupCronJob.class);

    protected static final String CLEANUP_CONVERSATION_JOB_NAME = "cleanupPostboxesJob";

    @Autowired
    private CassandraSimpleMessageCenterRepository postBoxRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Value("${replyts.cleanup.postboxes.schedule.expression:0 0/30 * * * ? *}")
    private String cronJobExpression;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    @Value("${comaas.cleanup.postbox.skipFailed:false}")
    private boolean skipFailed;

    @Override
    public void execute() {
        LOG.info("Started Cleanup Postbox Cronjob");

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
