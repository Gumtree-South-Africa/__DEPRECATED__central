package com.ecg.replyts.core.runtime.cron;


import com.ecg.replyts.core.api.cron.CronJobExecutor;

public class SampleCronJobExecutor implements CronJobExecutor {

    private String crontab;

    public SampleCronJobExecutor(String crontab) {
        this.crontab = crontab;
    }

    @Override
    public void execute() throws Exception {

    }

    @Override
    public String getPreferredCronExpression() {
        return crontab;
    }

}
