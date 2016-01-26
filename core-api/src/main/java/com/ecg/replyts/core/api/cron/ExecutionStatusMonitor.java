package com.ecg.replyts.core.api.cron;

public interface ExecutionStatusMonitor {
    Iterable<CronExecution> currentlyExecuted();
}