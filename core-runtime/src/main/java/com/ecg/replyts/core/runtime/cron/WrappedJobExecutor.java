package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class WrappedJobExecutor implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(WrappedJobExecutor.class);

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobKey jobKey = ctx.getJobDetail().getKey();
        String jobType = jobKey.getGroup();
        String jobName = jobKey.getName();

        try {
            LOG.trace("Executing Job {}:{}", jobType, jobName);

            Class<? extends CronJobExecutor> executorType = (Class<? extends CronJobExecutor>) Class.forName(jobName);
            CronJobService service = (CronJobService) ctx.getScheduler().getContext().get(CronJobService.CRON_JOB_SERVICE);

            service.invokeMonitoredIfLeader(executorType);

            LOG.trace("Job Execution Complete {}:{}", jobType, jobName);
        } catch (Exception e) {
            throw new RuntimeException(format("Failed executing cronjob %s", jobName), e);
        }
    }
}
