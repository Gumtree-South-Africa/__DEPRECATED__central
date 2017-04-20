package com.ecg.messagecenter.cronjobs;

import com.ecg.messagecenter.persistence.simple.CassandraSimplePostBoxRepository;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static org.joda.time.DateTime.now;

@Component
@ConditionalOnExpression("#{" +
            "'${replyts2.cleanup.postboxes.enabled}' == '${region}' && " +
            "('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid'))" +
        "}")
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

    @Override
    public void execute() throws Exception {
        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(maxConversationAgeDays, CLEANUP_CONVERSATION_JOB_NAME, DateTimeFieldType.hourOfDay());

        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting conversations for the date {}", cleanupDate);

        postBoxRepository.cleanup(cleanupDate);

        cronJobClockRepository.set(CLEANUP_CONVERSATION_JOB_NAME, now(), cleanupDate);

        LOG.info("Cleanup: Finished deleting conversations");
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }
}
