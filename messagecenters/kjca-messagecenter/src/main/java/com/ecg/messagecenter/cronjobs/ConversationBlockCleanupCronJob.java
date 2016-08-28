package com.ecg.messagecenter.cronjobs;

import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static org.joda.time.DateTime.now;

@Component
@ConditionalOnExpression("#{('${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')) && '${replyts2.cleanup.postboxes.enabled}' == 'true'}")
public class ConversationBlockCleanupCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationBlockCleanupCronJob.class);

    @Autowired
    private ConversationBlockRepository conversationBlockRepo;

    @Value("${replyts.maxConversationAgeDays}")
    private int maxAgeDays;

    @Override
    public void execute() throws Exception {
        DateTime deleteBlocksBefore = now(DateTimeZone.UTC).minusDays(maxAgeDays);

        LOG.info("Deleting user conversation blocks before '{}'", deleteBlocksBefore);

        try {
            conversationBlockRepo.cleanupOldConversationBlocks(deleteBlocksBefore);
        } catch (RuntimeException e) {
            LOG.error("Cleanup: user conversation blocks cleanup failed", e);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return everyNMinutes(30, 15);
    }
}
