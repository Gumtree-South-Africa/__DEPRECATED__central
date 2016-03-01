package com.ecg.de.ebayk.messagecenter.cronjobs;

import com.ecg.de.ebayk.messagecenter.persistence.PostBoxRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static org.joda.time.DateTime.now;

@Component
public class PostBoxCleanupCronJob implements CronJobExecutor {

    private final PostBoxRepository postBoxRepository;
    private final int maxAgeDays;

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxCleanupCronJob.class);

    @Autowired
    public PostBoxCleanupCronJob(PostBoxRepository postBoxRepository, @Value("${replyts.maxConversationAgeDays}") int maxAgeDays) {
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
        return everyNMinutes(37);
    }
}
