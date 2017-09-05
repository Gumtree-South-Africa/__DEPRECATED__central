package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.search.MutableSearchService;
import com.google.common.collect.Range;
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
@ConditionalOnExpression("#{'${replyts2.cronjob.cleanupSearch.enabled:true}' == 'true' || '${replyts2.cronjob.cleanupSearch.enabled:true}' == '${region}'}")
public class CleanupSearchCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupSearchCronJob.class);

    @Value("${replyts2.cronjob.cleanupSearch.minuteInterval:30}")
    private int minuteInterval;

    @Autowired
    private CleanupConfiguration config;

    @Autowired
    private MutableSearchService searchService;

    @Override
    public void execute() throws Exception {
        DateTime deleteEverythingBefore = now().minusDays(config.getMaxConversationAgeDays());

        LOG.info("Cleanup: Deleting SearchIndex older than {} days: everything before '{}'", config.getMaxConversationAgeDays(), deleteEverythingBefore);

        try {
            searchService.delete(Range.closed(new DateTime(0), now().minusDays(config.getMaxConversationAgeDays())));
        } catch (RuntimeException e) {
            LOG.error("Cleanup: ElasticSearch cleanup failed", e);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return everyNMinutes(minuteInterval);
    }
}