package com.ecg.replyts.core.runtime.cron;

import com.codahale.metrics.Gauge;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.runtime.TimingReports;
import com.hazelcast.core.HazelcastInstance;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * manages monitoring of cron jobs. each job has it's own check. when a check fails/succeeds,
 * {@link MonitoringSupport} is notified about this and puts the output of the last run into the check of the job.
 * Therefore the check is somewhat asynchronous.
 *
 * @author mhuttar
 */
class MonitoringSupport {

    private final Map<Class<?>, DistributedCronCheck> checks;

    static class CronRunningGauge implements Gauge<Integer> {

        private final DistributedCronCheck check;

        CronRunningGauge(DistributedCronCheck check) {
            checkNotNull(check);
            this.check = check;
        }

        @Override
        public Integer getValue() {
            return check.isRunning() ? 1 : 0;
        }
    }

    MonitoringSupport(List<CronJobExecutor> executors, HazelcastInstance hazelcast) {
        Map<Class<?>, DistributedCronCheck> tmp = new HashMap<>();
        for (CronJobExecutor cje : executors) {
            DistributedCronCheck check = new DistributedCronCheck(cje.getClass(), hazelcast);
            tmp.put(cje.getClass(), check);
            String gaugeName = String.format("running-cronjobs.%s", cje.getClass().getSimpleName());
            TimingReports.newGauge(gaugeName, new CronRunningGauge(check));
        }
        this.checks = Collections.unmodifiableMap(tmp);
    }

    List<Check> getChecks() {
        return new ArrayList<>(checks.values());
    }

    void start(Class<? extends CronJobExecutor> type) {
        checks.get(type).setRunning(true);
    }

    void success(Class<? extends CronJobExecutor> type) {

        completed(type, Optional.empty());
    }

    void failure(Class<? extends CronJobExecutor> type, Exception e) {
        completed(type, Optional.of(e));
    }

    private void completed(Class<? extends CronJobExecutor> type, Optional<Exception> e) {
        DistributedCronCheck check = checks.get(type);
        check.setRunning(false);
        check.setState(new Date(), e.orElse(null));
    }

}
