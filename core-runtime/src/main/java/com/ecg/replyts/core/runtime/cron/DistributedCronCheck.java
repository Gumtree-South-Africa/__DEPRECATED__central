package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IAtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;

/**
 * The state of this check will reflect the cluster wide state via Hazelcast.
 * <p/>
 * This ensures that every node in the cluster shows the cluster wide state so that "failed" checks won't
 * show stale status when the cron job already ran successful on another node.
 */
class DistributedCronCheck implements Check {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedCronCheck.class);

    private static final String LAST_RUN_CLUSTER_KEY = "-lastRun";
    private static final String LAST_EXCEPTION_CLUSTER_KEY = "-lastException";
    private static final String IS_RUNNING_CLUSTER_KEY = "-isRunning";

    private final String type;
    private final IAtomicReference<Long> lastRun; // Use AtomicReference instead of AtomicLong to be able detect null as never running.
    private final IAtomicReference<Exception> lastException;
    private final IAtomicReference<Boolean> isRunning;

    DistributedCronCheck(Class<?> classType, HazelcastInstance hazelcast) {
        type = classType.getSimpleName();
        String name = type + LAST_EXCEPTION_CLUSTER_KEY;
        lastException = hazelcast.getAtomicReference(name);
        lastRun = hazelcast.getAtomicReference(type + LAST_RUN_CLUSTER_KEY);
        isRunning = hazelcast.getAtomicReference(type + IS_RUNNING_CLUSTER_KEY);
    }

    @Override
    public Result execute() {
        Long lastRunValue = lastRun.get();
        Exception lastExceptionValue = lastException.get();

        Status s = Status.OK;
        Message msg;
        if (lastRunValue == null) {
            msg = Message.shortInfo("Not run yet");
        } else if (lastExceptionValue == null) {
            msg = Message.shortInfo(String.format("Successfully completed at %s", new Date(lastRunValue)));
        } else {
            s = Status.CRITICAL;
            msg = Message.fromException(String.format("Error in execution at %s", new Date(lastRunValue)), lastExceptionValue);
        }
        return Result.createResult(s, msg);
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
    }

    public boolean isRunning() {
        try {
            return Optional.ofNullable(isRunning.get()).orElse(false);
        } catch (HazelcastException | HazelcastInstanceNotActiveException hzExc) {
            // Not printing the entire stack trace here, because it's basically useless and will
            // flood the logs due to how often this method is called.
            LOG.warn("Can't fetch is-running status of check [{}]. Message: {}", getName(), hzExc.getMessage());
            return false;
        }
    }
}
