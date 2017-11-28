package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.indexer.Indexer;
import com.ecg.replyts.core.runtime.cron.CronExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("#{('${replyts2.cronjob.deltaindexer.enabled:true}' == 'true' || '${replyts2.cronjob.deltaindexer.enabled:true}' == '${region}') && '${persistence.strategy}' == 'riak'}")
public class DeltaIndexingCronJob implements CronJobExecutor {
    @Value("${replyts.deltaindexer.intervalMinutes:10}")
    private int intervalMinutes;

    @Value("${region:unknown}")
    private String region;

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
