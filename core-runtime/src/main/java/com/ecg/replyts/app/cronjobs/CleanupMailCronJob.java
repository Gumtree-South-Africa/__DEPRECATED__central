package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.persistence.MailRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static org.joda.time.DateTime.now;

@Component
@ConditionalOnProperty(name = "replyts2.cronjob.cleanupMail.enabled", havingValue = "true")
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class CleanupMailCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupMailCronJob.class);

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private CleanupConfiguration config;

    @Override
    public void execute() throws Exception {
        final DateTime deleteEverythingBefore = now().minusDays(config.getMaxMailAgeDays());

        LOG.info("Deleting Mails older than {} days: everything before '{}', maxResults: '{}'", config.getMaxMailAgeDays(), deleteEverythingBefore, config.getMaxResults());
        mailRepository.deleteMailsByOlderThan(deleteEverythingBefore, config.getMaxResults(), config.getNumCleanUpThreads());
    }

    @Override
    public String getPreferredCronExpression() {
        return everyNMinutes(config.getEveryNMinutes());
    }
}