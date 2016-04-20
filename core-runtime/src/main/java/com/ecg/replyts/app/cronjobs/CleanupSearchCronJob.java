package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.search.MutableSearchService;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.never;
import static org.joda.time.DateTime.now;

public class CleanupSearchCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupSearchCronJob.class);

    private final boolean cronJobEnabled;
    private final MutableSearchService searchService;
    private final int maxAgeDays;
    private final int minuteInterval;


    @Autowired
    CleanupSearchCronJob(
            @Value("${replyts2.cronjob.cleanupSearch.enabled:true}") boolean cronJobEnabled,
            MutableSearchService searchService,
            @Value("${replyts.maxConversationAgeDays}") int maxAgeDays,
            @Value("${replyts2.cronjob.cleanupSearch.minuteInterval:30}") int minuteInterval ) {
        this.cronJobEnabled = cronJobEnabled;
        this.searchService = searchService;
        this.maxAgeDays = maxAgeDays;
        this.minuteInterval = minuteInterval;
    }

    @Override
    public void execute() throws Exception {
        DateTime deleteEverythingBefore = now().minusDays(maxAgeDays);

        LOG.info("Deleting SearchIndex older than {} days: everything before '{}'", maxAgeDays, deleteEverythingBefore);

        try {
            searchService.delete(Range.closed(new DateTime(0), now().minusDays(maxAgeDays)));
        } catch (RuntimeException e) {
            LOG.error("Cleanup: ElasticSearch cleanup failed", e);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        if (!cronJobEnabled) {
            return never();
        }
        return everyNMinutes(minuteInterval);
    }
}