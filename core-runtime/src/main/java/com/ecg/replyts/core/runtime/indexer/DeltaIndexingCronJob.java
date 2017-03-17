package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.indexer.Indexer;
import com.ecg.replyts.core.runtime.cron.CronExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("#{'${replyts2.cronjob.deltaindexer.enabled:true}' == 'true'}")
public class DeltaIndexingCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DeltaIndexingCronJob.class);

    @Value("${replyts.deltaindexer.intervalMinutes:10}")
    private int intervalMinutes;

    @Autowired
    private Indexer indexer;

    @Override
    public void execute() throws Exception {
        indexer.deltaIndex();
    }

    @Override
    public String getPreferredCronExpression() {
        return CronExpressionBuilder.everyNMinutes(intervalMinutes);
    }
}
