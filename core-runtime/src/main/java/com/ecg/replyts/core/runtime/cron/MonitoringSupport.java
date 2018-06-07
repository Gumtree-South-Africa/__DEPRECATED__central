package com.ecg.replyts.core.runtime.cron;

import com.codahale.metrics.Gauge;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.runtime.TimingReports;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Component
@ConditionalOnProperty(value = "node.run.cronjobs", havingValue = "true")
public class MonitoringSupport {
    @Autowired(required = false)
    private List<CronJobExecutor> executors = Collections.emptyList();

    @Autowired
    private HazelcastInstance hazelcast;

    private Map<Class<?>, DistributedCronCheck> checks = new HashMap<>();

    @PostConstruct
    private void initialize() {
        for (CronJobExecutor executor : executors) {
            DistributedCronCheck check = new DistributedCronCheck(executor.getClass(), hazelcast);

            checks.put(executor.getClass(), check);
            TimingReports.newGauge(format("running-cronjobs.%s", executor.getClass().getSimpleName()), new CronRunningGauge(check));
        }
    }

    public List<Check> getChecks() {
        return new ArrayList<>(checks.values());
    }

    public void start(Class<? extends CronJobExecutor> type) {
        checks.get(type).setRunning(true);
    }

    public void success(Class<? extends CronJobExecutor> type) {
        completed(type, null);
    }

    public void failure(Class<? extends CronJobExecutor> type, Exception e) {
        completed(type, e);
    }

    private void completed(Class<? extends CronJobExecutor> type, Exception e) {
        DistributedCronCheck check = checks.get(type);

        check.setRunning(false);
        check.setState(new Date(), e);
    }

    static class CronRunningGauge implements Gauge<Integer> {
        private DistributedCronCheck check;

        CronRunningGauge(DistributedCronCheck check) {
            this.check = check;
        }

        @Override
        public Integer getValue() {
            return check.isRunning() ? 1 : 0;
        }
    }
}