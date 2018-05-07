package com.ecg.messagecenter.kjca.cronjobs;

import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static org.joda.time.DateTime.now;

/**
 * Unlike the ConversationThread-related cleanup cronjobs, this cronjob can be applied to both Riak and Cassandra as it
 * works in the same way. This is possible due to the relatively small number of blocks during the active window, which
 * lets us get away with doing full table-scans for Cassandra. This doesn't perform well enough for conversation threads
 * however.
 */
@Component
@ConditionalOnProperty(name = "CRONJOBS_ENABLED", havingValue = "true", matchIfMissing = false)
@ConditionalOnExpression("#{'${replyts2.cleanup.conversationblock.enabled:false}' == 'true' && '${active.dc}' != '${region}' && '${persistence.strategy}' == 'cassandra'}")
public class ConversationBlockCleanupCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationBlockCleanupCronJob.class);

    @Autowired
    private ConversationBlockRepository conversationBlockRepo;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    @Override
    public void execute() throws Exception {
        DateTime deleteBlocksBefore = now(DateTimeZone.UTC).minusDays(maxAgeDays);

        LOG.info("Cleanup: Deleting user conversation blocks before '{}'", deleteBlocksBefore);

        try {
            conversationBlockRepo.cleanup(deleteBlocksBefore);
        } catch (RuntimeException e) {
            LOG.error("Cleanup: user conversation blocks cleanup failed", e);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return everyNMinutes(30, 15);
    }
}