package com.ecg.messagecenter.cronjobs;

import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static org.joda.time.DateTime.now;

public class PostBoxCleanupCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(PostBoxCleanupCronJob.class);

    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    @Override
    public void execute() throws Exception {
        int cleanupDayBoundary = maxAgeDays + 1; // +1 to not collide "boundary still valid conversations"

        DateTime deletePostBoxesBefore = now().minusDays(cleanupDayBoundary);

        LOG.info("Deleting PostBoxes untouched for more than {} days: everything before '{}'", cleanupDayBoundary, deletePostBoxesBefore);

        try {
            postBoxRepository.cleanupLongTimeUntouchedPostBoxes(deletePostBoxesBefore);
        } catch (RuntimeException e) {
            LOG.error("Cleanup: PostBox cleanup failed", e);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return everyNMinutes(37);
    }
}