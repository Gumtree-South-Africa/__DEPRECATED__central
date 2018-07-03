package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronExecution;
import com.ecg.replyts.core.api.cron.ExecutionStatusMonitor;
import com.google.common.collect.ImmutableList;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@ConditionalOnProperty(value = "node.run.cronjobs", havingValue = "true")
public class DistributedExecutionStatusMonitor implements ExecutionStatusMonitor {
    @Autowired
    private HazelcastInstance hazelcast;

    private List<CronExecution> runningExecutions;

    @PostConstruct
    private void initialize() {
        this.runningExecutions = hazelcast.getList("distributed-execution-status-monitor");
    }

    @Override
    public Iterable<CronExecution> currentlyExecuted() {
        return ImmutableList.copyOf(runningExecutions);
    }

    public void start(CronExecution e) {
        runningExecutions.add(e);
    }

    public void end(CronExecution e) {
        runningExecutions.remove(e);
    }
}