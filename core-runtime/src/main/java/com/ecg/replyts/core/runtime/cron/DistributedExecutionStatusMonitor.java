package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronExecution;
import com.ecg.replyts.core.api.cron.ExecutionStatusMonitor;
import com.google.common.collect.ImmutableList;
import com.hazelcast.core.HazelcastInstance;

import java.util.List;

public class DistributedExecutionStatusMonitor implements ExecutionStatusMonitor {

    private final List<CronExecution> runningExecutions;

    public DistributedExecutionStatusMonitor(HazelcastInstance hazelcast) {
        this(hazelcast.<CronExecution>getList("distributed-execution-status-monitor"));
    }

    public DistributedExecutionStatusMonitor(List<CronExecution> set) {
        this.runningExecutions = set;
    }

    @Override
    public Iterable<CronExecution> currentlyExecuted() {
        return ImmutableList.copyOf(runningExecutions);
    }

    void start(CronExecution e) {
        runningExecutions.add(e);
    }

    void end(CronExecution e) {
        runningExecutions.remove(e);
    }


}
