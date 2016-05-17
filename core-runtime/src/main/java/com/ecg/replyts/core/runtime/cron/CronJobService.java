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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service capable of executing cron jobs. This service uses quartz internally to ensure cron jobs are executed uniquely
 * per cluster, not per machine. This service will pick up all Spring Beans that implement {@link CronJobExecutor} and
 * register them to quartz with their {@link CronJobExecutor#getPreferredCronExpression()}. <br/>
 * Also provides monitoring for all jobs by exporting one {@link Check} per {@link CronJobExecutor} that will be updated
 * on each job execution.<br/>
 * Registers MBeans for all Jobs to make them invokable via JMX. <br/>
 * Registers Sanity checks for all Cron Jobs that are registered.
 *
 * @author mhuttar
 */
@Service
public class CronJobService implements CheckProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CronJobService.class);

    protected static final String CRON_JOB_SERVICE = CronJobService.class.getName();

    private final MonitoringSupport monitoringSupport;

    private final Map<Class<? extends CronJobExecutor>, CronJobExecutor> cronReg;

    private final DistributedExecutionStatusMonitor statusMonitor;

    private final HazelcastInstance hazelcast;

    private Scheduler scheduler;

    private JmxInvokeSupport jmxInvokeSupport;

    private final boolean disableCronjobs;


    /**
     * Initializes the {@link CronJobService} and registers all given {@link CronJobExecutor} to the embedded quartz
     * scheduler.
     *
     * @param executors
     * @param disableCronjobs
     */
    @Autowired
    public CronJobService(
      @Value("${cluster.jmx.enabled:true}") boolean isJmxEnabled,
      List<CronJobExecutor> executors,
      DistributedExecutionStatusMonitor statusMonitor,
      HazelcastInstance hazelcast,
      @Value("${node.passive:false}") boolean disableCronjobs) {
        this.statusMonitor = statusMonitor;
        this.hazelcast = hazelcast;
        this.disableCronjobs = disableCronjobs;

        try {
            LOG.info("Initializing Cron Job Frameworks. Registered Cronjobs: {}", executors);

            monitoringSupport = new MonitoringSupport(executors, hazelcast);

            cronReg = sortExecutors(executors);

            if (!disableCronjobs) {
                scheduler = new StdSchedulerFactory().getScheduler();

                initExecutors();

                scheduler.getContext().put(CRON_JOB_SERVICE, this);

                scheduler.start();

                LOG.debug("Cron jobs initialized");
            } else {
                LOG.debug("Cron jobs disabled");
            }

            jmxInvokeSupport = isJmxEnabled ? new JmxInvokeSupport(this, executors) : null;
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Class<? extends CronJobExecutor>, CronJobExecutor> sortExecutors(List<CronJobExecutor> executors) {
        Map<Class<? extends CronJobExecutor>, CronJobExecutor> instances = new HashMap<Class<? extends CronJobExecutor>, CronJobExecutor>();
        for (CronJobExecutor c : executors) {
            if (instances.containsKey(c.getClass())) {
                throw new IllegalStateException("Found Multiple Jobs of same type for Cron Job " + c);
            }
            instances.put(c.getClass(), c);
        }
        return Collections.unmodifiableMap(instances);
    }


    private void initExecutors() throws SchedulerException {
        LOG.info("Initializing Cron Jobs");
        if (disableCronjobs) {
            LOG.warn("Cronjobs disabled - this is a passive node!");
            return;
        }
        for (CronJobExecutor cje : cronReg.values()) {
            String jobName = cje.getClass().getName();
            JobDetail jobDetail = JobBuilder.newJob(WrappedJobExecutor.class)
                    .withIdentity(jobName, "ReplyTS")
                    .build();

            if (scheduler.checkExists(jobDetail.getKey())) {
                LOG.info("Reusing existing Cron Job configuration for job {}", jobDetail.getKey());
                continue;
            }
            LOG.info("Creating new Cron Job for {}, fire at {}", cje.getClass(), cje.getPreferredCronExpression());
            CronTrigger jobSchedule = TriggerBuilder
                    .newTrigger()
                    .withIdentity(jobName + ".trigger", "ReplyTS")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cje.getPreferredCronExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, jobSchedule);
        }
    }

    /**
     * Invokes a job and tries to ensure that this job is only executed once per cluster rather than on all nodes.
     * Informs the job's monitor about it's outcome.
     *
     * @param type
     */
    void invokeMonitoredIfLeader(Class<? extends CronJobExecutor> type) {
        ILock clusterJobLock = hazelcast.getLock(type.getName());
        boolean executeJobOnThisNode = clusterJobLock.tryLock();
        if (executeJobOnThisNode) {
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

    /**
     * Invokes a Cronjob and informs the job's monitor about it's outcome.
     *
     * @param type
     */
    void invokeMonitored(Class<? extends CronJobExecutor> type) {
        CronJobExecutor cronJobExecutor = cronReg.get(type);
        if (cronJobExecutor == null) {
            LOG.error("No Job of type " + type + " available. ");
            return;
        }
        try {
            LOG.info("Invoking Cron Job {}", type);
            monitoringSupport.start(type);
            Stopwatch timing = Stopwatch.createStarted();
            cronJobExecutor.execute();
            timing.stop();
            monitoringSupport.success(type);
            LOG.info("Cron Job {} completed in {}", type, timing.toString());
        } catch (Exception e) {
            LOG.error("Cron Job aborted Abnormally: " + type, e);
            monitoringSupport.failure(type, e);
        }

    }


    /**
     * shuts down the cron job execution.
     */
    @PreDestroy
    void stop() {
        try {
            if (jmxInvokeSupport != null) {
                jmxInvokeSupport.stop();
            }

            if (scheduler != null) {
                scheduler.shutdown();

                LOG.info("Cron Job Service shutdown");
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Check> getChecks() {
        return monitoringSupport.getChecks();
    }
}
