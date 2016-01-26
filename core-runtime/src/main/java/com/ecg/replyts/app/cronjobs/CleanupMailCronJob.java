package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.persistence.MailRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;
import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.never;
import static org.joda.time.DateTime.now;

public class CleanupMailCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupMailCronJob.class);

    private final boolean cronJobEnabled;
    private final MailRepository mailRepository;
    private CleanupConfiguration config;

    @Autowired
    CleanupMailCronJob(
            @Value("${replyts2.cronjob.cleanupMail.enabled:true}") boolean cronJobEnabled,
            MailRepository mailRepository,
            CleanupConfiguration config) {
        this.cronJobEnabled = cronJobEnabled;
        this.mailRepository = mailRepository;
        this.config = config;
    }

    @Override
    public void execute() throws Exception {
        final DateTime deleteEverythingBefore = now().minusDays(config.getMaxMailAgeDays());

        LOG.info("Deleting Mails older than {} days: everything before '{}', maxResults: '{}'", config.getMaxMailAgeDays(), deleteEverythingBefore, config.getMaxResults());
        mailRepository.deleteMailsByOlderThan(deleteEverythingBefore, config.getMaxResults(), config.getNumCleanUpThreads());
    }

    @Override
    public String getPreferredCronExpression() {
        if (!cronJobEnabled) {
            return never();
        }
        return everyNMinutes(config.getEveryNMinutes());
    }
}