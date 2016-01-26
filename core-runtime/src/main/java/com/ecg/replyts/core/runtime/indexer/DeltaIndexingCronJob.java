package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.indexer.Indexer;
import com.ecg.replyts.core.runtime.cron.CronExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class DeltaIndexingCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(DeltaIndexingCronJob.class);

    private final int intervalMinutes;
    private final Indexer indexer;

    @Autowired
    DeltaIndexingCronJob(Indexer indexer, @Value("${replyts.deltaindexer.intervalMinutes:10}") int intervalMinutes) {
        this.indexer = indexer;
        this.intervalMinutes = intervalMinutes;
        LOG.info("Delta Indexing Cronjob executes every {} minutes. ", intervalMinutes);
    }

    @Override
    public void execute() throws Exception {
        indexer.deltaIndex();
    }

    @Override
    public String getPreferredCronExpression() {
        return CronExpressionBuilder.everyNMinutes(intervalMinutes);
    }
}
