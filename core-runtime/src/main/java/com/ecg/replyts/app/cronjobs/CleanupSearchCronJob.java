package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.search.MutableSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "CRONJOBS_ENABLED", havingValue = "true", matchIfMissing = false)
@ConditionalOnExpression("#{'${cronjob.cleanupSearch.enabled:false}' == 'true'}")
public class CleanupSearchCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupSearchCronJob.class);

    @Value("${replyts2.cronjob.cleanupSearch.schedule.expression:0 0 0 * * ?}")
    private String cronJobExpression;

    private final CleanupConfiguration config;

    private final MutableSearchService searchService;

    @Autowired
    public CleanupSearchCronJob(CleanupConfiguration config, MutableSearchService searchService) {
        this.config = config;
        this.searchService = searchService;
        LOG.warn("Cronjob started");
    }

    @Override
    public void execute() throws Exception {
        LocalDate now = now();
        LocalDate deleteEverythingBefore = now.minusDays(config.getMaxConversationAgeDays());

        LOG.info("Cleanup: Deleting SearchIndex older than {} days: everything before '{}'", config.getMaxConversationAgeDays(), deleteEverythingBefore);

        try {
            searchService.deleteModifiedAt(LocalDate.of(1970,1,1), deleteEverythingBefore);
        } catch (RuntimeException e) {
            LOG.error("Cleanup: ElasticSearch cleanup failed", e);
        }
    }

    protected LocalDate now() {
        return LocalDate.now();
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }
}