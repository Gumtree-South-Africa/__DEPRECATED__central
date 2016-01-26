package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.google.common.base.Optional;

import javax.management.ObjectName;

import static com.ecg.replyts.core.api.sanitychecks.Result.createResult;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;


/**
 * Abstract adapter to wrap a single check as MBean.
 *
 * @author smoczarski
 */
abstract class AbstractCheckAdapter implements CheckAdapter {

    private volatile Optional<Result> latestResult = absent();

    private ObjectName objectName;

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        if (!latestResult.isPresent()) return Status.WARNING.toString();
        return latestResult.get().status().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getMessage() {
        if (!latestResult.isPresent()) return "No checks executed yet!";
        return latestResult.get().value().toString();
    }

    public Result getLatestResult() {
        return latestResult.or(createResult(Status.WARNING, Message.shortInfo("Check not run yet")));
    }

    /**
     * {@inheritDoc}
     */
    public void execute() {
        Optional<Result> previousResult = latestResult;
        Optional<Result> newOutcome;
        try {
            newOutcome = of(performInternal());
        } catch (Exception e) {
            newOutcome = of(createResult(Status.CRITICAL, Message.fromException(e)));
        }
        latestResult = newOutcome;

        if (!previousResult.isPresent() || previousResult.get().status() != newOutcome.get().status()) {
            SwitchLogger.log(getName(), previousResult, newOutcome.get());
        }
    }


    /**
     * @return The JMX name.
     */
    public abstract String getName();


    /**
     * Perform the check.
     *
     * @return The result of the check.
     * @throws Exception If an exception occurs during the check.
     */
    protected abstract Result performInternal() throws Exception;


    /**
     * @return the objectName
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /**
     * @param objectName the objectName to set
     */
    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

}
