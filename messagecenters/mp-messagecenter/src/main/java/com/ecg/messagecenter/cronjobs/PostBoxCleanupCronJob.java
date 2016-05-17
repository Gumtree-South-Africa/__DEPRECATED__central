package com.ecg.messagecenter.cronjobs;

import com.ecg.messagecenter.persistence.riak.RiakPostBoxRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.never;
import static org.joda.time.DateTime.now;

public class PostBoxCleanupCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxCleanupCronJob.class);

    private final boolean cronJobEnabled;
    private final RiakPostBoxRepository postBoxRepository;
    private final int maxAgeDays;

    @Autowired
    public PostBoxCleanupCronJob(
            @Value("${replyts2.cronjob.cleanupPostbox.enabled:true}") boolean cronJobEnabled,
            RiakPostBoxRepository postBoxRepository,
            @Value("${replyts.maxConversationAgeDays}") int maxAgeDays) {
        this.cronJobEnabled = cronJobEnabled;
        this.postBoxRepository = postBoxRepository;
        this.maxAgeDays = maxAgeDays;
    }

    @Override
    public void execute() throws Exception {
        // +1 to not collide "boundary still valid conversations"
        int cleanupDayBoundary = maxAgeDays + 1;
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
        if (!cronJobEnabled) {
            return never();
        }
        return everyNMinutes(37);
    }
}