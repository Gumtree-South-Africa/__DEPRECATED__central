package com.ecg.messagecenter.cronjobs;

import com.ecg.messagecenter.persistence.simple.RiakSimplePostBoxRepository;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.joda.time.DateTime;
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
public class RiakSimplePostBoxCleanupCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(RiakSimplePostBoxCleanupCronJob.class);

    @Autowired
    private RiakSimplePostBoxRepository postBoxRepository;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    @Override
    public void execute() throws Exception {
        int cleanupDayBoundary = maxAgeDays + 1; // +1 to not collide "boundary still valid conversations"

        DateTime deletePostBoxesBefore = now().minusDays(cleanupDayBoundary);

        LOG.info("Deleting PostBoxes untouched for more than {} days: everything before '{}'", cleanupDayBoundary, deletePostBoxesBefore);

        try {
            postBoxRepository.cleanup(deletePostBoxesBefore);
        } catch (RuntimeException e) {
            LOG.error("Cleanup: PostBox cleanup failed", e);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return everyNMinutes(37);
    }
}