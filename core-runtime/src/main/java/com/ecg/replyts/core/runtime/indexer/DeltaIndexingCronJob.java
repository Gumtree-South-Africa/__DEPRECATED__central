package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.indexer.Indexer;
import com.ecg.replyts.core.runtime.cron.CronExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.never;

class DeltaIndexingCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(DeltaIndexingCronJob.class);

    private final boolean cronJobEnabled;
    private final int intervalMinutes;
    private final Indexer indexer;

    @Autowired
    DeltaIndexingCronJob(
            @Value("${replyts2.cronjob.deltaindexer.enabled:true}") boolean cronJobEnabled,
            Indexer indexer,
            @Value("${replyts.deltaindexer.intervalMinutes:10}") int intervalMinutes) {
        this.cronJobEnabled = cronJobEnabled;
        this.indexer = indexer;
        this.intervalMinutes = intervalMinutes;
        if (cronJobEnabled) {
            LOG.info("Delta Indexing Cronjob executes every {} minutes", intervalMinutes);
        } else {
            LOG.info("Delta Indexing Cronjob is disabled");
        }
    }

    @Override
    public void execute() throws Exception {
        indexer.deltaIndex();
    }

    @Override
    public String getPreferredCronExpression() {
        if (!cronJobEnabled) {
            return never();
        }
        return CronExpressionBuilder.everyNMinutes(intervalMinutes);
    }
}
