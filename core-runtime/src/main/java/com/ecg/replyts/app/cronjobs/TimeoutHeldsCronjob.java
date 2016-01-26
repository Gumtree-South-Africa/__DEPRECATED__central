package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.runtime.cron.CronExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

class TimeoutHeldsCronjob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(TimeoutHeldsCronjob.class);

    private final boolean cronJobEnabled;
    private final Timeframe workingSlot;
    private final MessageSender messageSender;

    @Autowired
    TimeoutHeldsCronjob(
            @Value("${replyts2.sendHeld.timeoutEnabled:false}") boolean cronJobEnabled,
            MessageSender messageSender,
            @Qualifier("timeoutHeldsCronJobTimeframe") Timeframe timeframe
    ) {
        this.cronJobEnabled = cronJobEnabled;
        this.messageSender = messageSender;
        this.workingSlot = timeframe;

        if (cronJobEnabled) {
            LOG.info("ENABLED Auto Sending of Held Mails after retention time. Agents Working Hours: {}h-{}h. Retention time: {}h",
                    timeframe.getCsWorkingHoursStart(),
                    timeframe.getCsWorkingHoursEnd(),
                    timeframe.getRetentionTime());
        } else {
            LOG.info("DISABLED Auto Sending of Held Mails after retention time");
        }
    }

    @Override
    public void execute() throws Exception {
        if (workingSlot.operateNow()) {
            // agents are working right now, we do not want to autosend mails.
            messageSender.work();
        }
    }

    @Override
    public String getPreferredCronExpression() {
        // feature is disabled, we do not need this cronjob.
        if (!cronJobEnabled) {
            return CronExpressionBuilder.never();
        }
        return CronExpressionBuilder.everyNMinutes(30);
    }
}