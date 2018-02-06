package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This check ensures that every node in the cluster shows the cluster wide state, so that "failed" checks won't
 * show a stale status when the cronjob already ran successfully on another node.
 */
public class DistributedCronCheck implements Check {
    private static final String LAST_RUN_CLUSTER_KEY = "-lastRun";
    private static final String LAST_EXCEPTION_CLUSTER_KEY = "-lastException";

    private final String type;
    private final IAtomicReference<Long> lastRun; // Use AtomicReference instead of AtomicLong to be able detect null as never running.
    private final IAtomicReference<Exception> lastException;
    private final AtomicBoolean isRunning = new AtomicBoolean();
    private final AtomicBoolean isFirstReport = new AtomicBoolean();

    public DistributedCronCheck(Class<?> classType, HazelcastInstance hazelcast) {
        type = classType.getSimpleName();
        lastException = hazelcast.getAtomicReference(type + LAST_EXCEPTION_CLUSTER_KEY);
        lastRun = hazelcast.getAtomicReference(type + LAST_RUN_CLUSTER_KEY);
    }

    @Override
    public Result execute() {
        Long lastRunValue = lastRun.get();
        Exception lastExceptionValue = lastException.get();

        if (lastRunValue == null) {
            return Result.createResult(Status.OK, Message.shortInfo("Not run yet"));
        } else if (lastExceptionValue == null) {
            return Result.createResult(Status.OK, Message.shortInfo(String.format("Successfully completed at %s", new Date(lastRunValue))));
        } else {
            return Result.createResult(Status.CRITICAL, Message.fromException(String.format("Error in execution at %s", new Date(lastRunValue)), lastExceptionValue));
        }
    }

    public void setState(Date lastRun, Exception e) {
        this.lastRun.set(lastRun.getTime());
        this.lastException.set(e);
    }

    @Override
    public String getName() {
        return type;
    }

    @Override
    public String getCategory() {
        return "ReplyTS";
    }

    @Override
    public String getSubCategory() {
        return "CronJob";
    }

    public void setRunning(boolean value) {
        isRunning.set(value);
        if (value) {
            isFirstReport.set(true);
        }
    }

    public boolean isRunning() {
        return isFirstReport.getAndSet(false) || isRunning.get();
    }
}