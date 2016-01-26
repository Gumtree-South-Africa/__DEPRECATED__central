package com.ecg.replyts.core.api.cron;

/**
 * Contract for Cron jobs that are to be executed regularly. Cron Jobs must define a {@link #getPreferredCronExpression()}.
 * this crontab will be a quartz style cron expression (in second granularity). To be picked up by the cron framework,
 * all implementations need to be spring beans. There may only be one Spring bean per type (although this might be
 * pretty obvious)
 *
 * @author mhuttar
 */
public interface CronJobExecutor {


    /**
     * executes the job. The job is guaranteed to be executed only once in the whole ReplyTS cluster.
     *
     * @throws Exception in case job execution went wrong. That will be reported to monitoring - the node's status will become CRITICAL.
     */
    void execute() throws Exception;

    /**
     * @return quartz style cron expression (in second granularity) when the job wants to be invoked. Details on
     * quartz style cron expressions can be found at http://quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06
     */
    String getPreferredCronExpression();
}