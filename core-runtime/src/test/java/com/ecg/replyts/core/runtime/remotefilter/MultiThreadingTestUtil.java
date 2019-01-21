package com.ecg.replyts.core.runtime.remotefilter;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.Assert.fail;

public class MultiThreadingTestUtil {

    /**
     * Test a runnable satisfies all of the following requirements:
     * - runnable is interruptible directly after starting
     * - runnable throws an Exception that satisfies given isCorrectException Predicate
     * - runnable responds well within maxWaitTime
     *
     * <p>
     * The predicate is tested in the Thread of the runnable, so if you want to test for the Interrupt flag, you can.
     * <p>
     * Note that the runnable may be interruptible at first, but later block on a method that is *not* interruptible.
     * This is something we cannot help test.
     */
    public static void assertThatCallableIsInterruptible(Callable runnable,
                                                         Predicate<Throwable> isCorrectException,
                                                         Duration maxWaitTime) {
        CountDownLatch threadStarted = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        // cannot Junit.fail() in a subthread, so make it set failure msg instead
        AtomicReference<RuntimeException> failure = new AtomicReference(new RuntimeException("thread was never started"));

        Thread t = new Thread(() -> {
            failure.set(new RuntimeException("Thread is still running"));
            threadStarted.countDown();
            try {
                runnable.call();
                // this is not good, fail at the end
            } catch (Exception e) {
                if (isCorrectException.test(e)) {
                    failure.set(null); // this is what we wanted
                    wasInterrupted.set(true);
                    return;
                }
                failure.set(new RuntimeException("InterruptibleFilter threw a " + e.getClass() + ", but that doesn't signal an Interrupted exception according to the contract", e));
                return;
            }

            // not good! should have thrown an exception
            failure.set(new RuntimeException("Filter finished succesfully, but should have been interrupted within response delay"));
        });

        t.start();

        // wait until thread started
        try {
            threadStarted.await();

            // execute trigger for System under Test
            t.interrupt();

            // wait for thread to finish
            t.join(maxWaitTime.toMillis());
        } catch (InterruptedException e) {
            // we can just declare this in the method signature, but that would be confusing as we are testing interrupts
            fail("main thread should not be interrupted");
        }

        // validate that interrupt was received in the other thread

        if (failure.get() != null) {
            throw failure.get();
        }

        if (!wasInterrupted.get()) { // redundant (failure == null) but explicit
            throw new RuntimeException("No interrupt signal was received");
        }
    }
}
