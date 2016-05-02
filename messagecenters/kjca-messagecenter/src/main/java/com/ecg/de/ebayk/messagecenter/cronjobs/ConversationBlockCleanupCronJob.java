package com.ecg.de.ebayk.messagecenter.cronjobs;

import com.ecg.de.ebayk.messagecenter.persistence.ConversationBlockRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static org.joda.time.DateTime.now;

@Component
public class ConversationBlockCleanupCronJob implements CronJobExecutor {

    private final ConversationBlockRepository conversationBlockRepo;
    private final int maxAgeDays;

    private static final Logger LOG = LoggerFactory.getLogger(ConversationBlockCleanupCronJob.class);

    @Autowired
    public ConversationBlockCleanupCronJob(
            ConversationBlockRepository conversationBlockRepo,
            @Value("${replyts.maxConversationAgeDays}") int maxAgeDays
    ) {
        this.conversationBlockRepo = conversationBlockRepo;
        this.maxAgeDays = maxAgeDays;
    }

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
