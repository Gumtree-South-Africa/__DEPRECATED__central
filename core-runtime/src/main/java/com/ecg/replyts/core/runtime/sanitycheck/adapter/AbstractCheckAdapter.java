package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;

import javax.management.ObjectName;

import java.util.Optional;

import static com.ecg.replyts.core.api.sanitychecks.Result.createResult;

abstract class AbstractCheckAdapter implements CheckAdapter {

    private volatile Optional<Result> latestResult = Optional.empty();

    private ObjectName objectName;

    public String getStatus() {
        return latestResult.map(result -> result.status().toString())
                .orElse(Status.WARNING.toString());
    }

    public String getMessage() {
        return latestResult.map(result -> result.value().toString())
                .orElse("No checks executed yet!");
    }

    public Result getLatestResult() {
        return latestResult.orElse(createResult(Status.WARNING, Message.shortInfo("Check not run yet")));
    }

    public void execute() {
        Optional<Result> previousResult = latestResult;
        Optional<Result> newOutcome;
        try {
            newOutcome = Optional.of(performInternal());
        } catch (Exception e) {
            newOutcome = Optional.of(createResult(Status.CRITICAL, Message.fromException(e)));
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

    protected abstract Result performInternal() throws Exception;

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }
}
