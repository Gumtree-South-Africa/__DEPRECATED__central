package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronExecution;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import com.google.common.base.Stopwatch;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.Collections;
import java.util.List;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;

@Service
@ConditionalOnProperty(value = "node.passive", havingValue = "false", matchIfMissing = true)
public class CronJobService implements CheckProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CronJobService.class);

    protected static final String CRON_JOB_SERVICE = CronJobService.class.getName();

    @Autowired
    private HazelcastInstance hazelcast;

    @Autowired
    private DistributedExecutionStatusMonitor statusMonitor;

    @Autowired(required = false)
    private List<CronJobExecutor> executors = Collections.emptyList();

    @Autowired
    private MonitoringSupport monitoringSupport;

    private Scheduler scheduler;

    @PostConstruct
    private void initialize() {
        LOG.info("Initializing cronjobs: {}", executors);

        try {
            scheduler = new StdSchedulerFactory().getScheduler();

            for (CronJobExecutor executor : executors) {
                String jobName = executor.getClass().getName();

                JobDetail jobDetail = JobBuilder.newJob(WrappedJobExecutor.class)
                  .withIdentity(jobName, "ReplyTS")
                  .build();

                LOG.info("Creating new cronjob for {} which fires at {}", executor.getClass(), executor.getPreferredCronExpression());

                CronTrigger jobSchedule = TriggerBuilder.newTrigger()
                  .withIdentity(jobName + ".trigger", "ReplyTS")
                  .withSchedule(CronScheduleBuilder.cronSchedule(executor.getPreferredCronExpression()))
                  .build();

                scheduler.scheduleJob(jobDetail, jobSchedule);
            }

            scheduler.getContext().put(CRON_JOB_SERVICE, this);

            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    public void invokeMonitoredIfLeader(Class<? extends CronJobExecutor> type) {
        ILock clusterJobLock = hazelcast.getLock(type.getName());

        if (clusterJobLock.tryLock()) {
            CronExecution cronExecution = CronExecution.forJob(type);

            try {
                statusMonitor.start(cronExecution);
                invokeMonitored(type);
            } finally {
                clusterJobLock.unlock();
                statusMonitor.end(cronExecution);
            }
        }
    }

    private void invokeMonitored(Class<? extends CronJobExecutor> type) {
        CronJobExecutor executor = executors.stream()
          .filter(e -> e.getClass().equals(type))
          .findFirst().orElseThrow(RuntimeException::new);

        String jobType = type.getSimpleName();

        try {
            setTaskFields(jobType);

            LOG.trace("Invoking cronjob {}", jobType);

            monitoringSupport.start(type);
            Stopwatch timing = Stopwatch.createStarted();

            executor.execute();

            timing.stop();
            monitoringSupport.success(type);

            LOG.info("Cronjob {} completed in {}", type, timing);
        } catch (Exception e) {
            LOG.error("Cronjob aborted abnormally: {}", jobType, e);

            monitoringSupport.failure(type, e);
        }
    }

    @Override
    public List<Check> getChecks() {
        return monitoringSupport.getChecks();
    }

    @PreDestroy
    private void stop() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}