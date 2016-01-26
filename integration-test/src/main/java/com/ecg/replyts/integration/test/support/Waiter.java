package com.ecg.replyts.integration.test.support;

import org.joda.time.DateTime;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public final class Waiter {
    private final Callable<Boolean> successCondition;

    private Waiter(Callable<Boolean> successCondition) {
        this.successCondition = successCondition;
    }

    public static Waiter await(Callable<Boolean> successCondition) {
        return new Waiter(successCondition);
    }

    public void within(long val, TimeUnit unit) {
        DateTime end = DateTime.now().plusMillis((int) unit.toMillis(val));
        do {
            try {
                if (successCondition.call()) {
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception happened while waiting", e);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (end.isAfterNow());
        throw new AssertionError("Waiting for " + successCondition.getClass() + " did not happen in " + val + " " + unit);
    }
}
