package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnExpression("#{'${cronjob.sendHeld.timeout.runIn}' == '${region}'}")
public class TimeoutHeldsCronjob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(TimeoutHeldsCronjob.class);

    private final Timeframe workingSlot;

    private final MessageSender messageSender;

    private final String cronJobExpression;

    @Autowired
    public TimeoutHeldsCronjob(Timeframe workingSlot, MessageSender messageSender,
                               @Value("${cronjob.sendHeld.expression:0 0/30 * * * ? *}") String cronJobExpression) {
        this.workingSlot = workingSlot;
        this.messageSender = messageSender;
        this.cronJobExpression = cronJobExpression;
    }

    @PostConstruct
    public void log() {
        LOG.info("ENABLED Auto Sending of Held Mails after retention time. Agents Working Hours: {}h-{}h. Retention time: {}h",
                workingSlot.getCsWorkingHoursStart(),
                workingSlot.getCsWorkingHoursEnd(),
                workingSlot.getRetentionTime());
    }

    @Override
    public void execute() throws Exception {
        if (workingSlot.operateNow()) {
            LOG.info("Starting a scheduled held mail removal");
            // Agents are working right now, we do not want to auto-send mails
            messageSender.work();
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }
}