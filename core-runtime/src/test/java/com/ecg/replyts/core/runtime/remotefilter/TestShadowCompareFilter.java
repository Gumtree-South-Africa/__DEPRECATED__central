package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class TestShadowCompareFilter {
    private static FilterFeedback resultA = new FilterFeedback("ok", "looks fine", 1, FilterResultState.OK);
    private static FilterFeedback resultB = new FilterFeedback("hold", "hold it", 1000, FilterResultState.HELD);

    /**
     * Failures are registered here because junit fail() doesn't work in different thread
     */
    private List<Exception> failures = new CopyOnWriteArrayList();

    private FilterWithShadowComparison.DifferenceReporter failWhenDifferenceIsReported = (a, b) -> {
        failures.add(new RuntimeException(String.format("difference detected: %s != %s", a, b)));
    };

    private AtomicBoolean differenceReportedSignal = new AtomicBoolean();

    // our filter stubs don't care about the context
    private MessageProcessingContext context = null;
    private static Duration comparisonTimeout100ms = Duration.ofMillis(100);

    public void assertNoFailuresOccured() {
        failures.stream().forEach(e -> {
            fail(e.getMessage());
        });
    }

    @Test
    public void completesOkAndActualResultReturned() {
        FilterWithShadowComparison sut = FilterWithShadowComparison.createForTesting(
                FilterStubs.delayedOK(0, resultA),
                FilterStubs.delayedOK(0, resultA),
                failWhenDifferenceIsReported,
                comparisonTimeout100ms
        );

        List<FilterFeedback> actual = sut.filter(context);

        assertNoFailuresOccured();
        assertEquals(Collections.singletonList(resultA), actual);
    }

    @Test
    public void slowTestFilterIsInterruptedAndActualResultReturned() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        CountDownLatch threadRunning = new CountDownLatch(1);

        InterruptibleFilter slowFilterThatMustBeInterruptedBySUT = context -> {
            try {
                Thread.sleep(10_000);
                wasInterrupted.set(false);
            } catch (InterruptedException e) {
                wasInterrupted.set(true); // this should happen

                // satisfies contract, be cannot be validated by test (as SUT doesn't expose this exception)
                throw InterruptibleFilter.createInterruptedException();
            } finally {
                threadRunning.countDown();
            }
            return Collections.singletonList(resultB);
        };

        FilterWithShadowComparison sut = FilterWithShadowComparison.createForTesting(
                FilterStubs.delayedOK(0, resultA),
                slowFilterThatMustBeInterruptedBySUT,
                failWhenDifferenceIsReported, // even though the results are different, the 2nd filter should never finish when interrupted
                comparisonTimeout100ms
        );

        List<FilterFeedback> actual = sut.filter(context);

        threadRunning.await(1000, TimeUnit.MILLISECONDS);

        assertNoFailuresOccured();
        assertTrue("interrupted should have been set by thread", wasInterrupted.get());
        assertEquals(Collections.singletonList(resultA), actual);
    }

    @Test
    public void failureOfReferenceFilterIsIgnoredAndActualResultReturned() {
        FilterWithShadowComparison sut = FilterWithShadowComparison.createForTesting(
                FilterStubs.delayedOK(0, resultA),
                FilterStubs.throwingRTE(50),
                failWhenDifferenceIsReported,
                comparisonTimeout100ms
        );

        List<FilterFeedback> actual = sut.filter(context);
        assertNoFailuresOccured();
        assertEquals(Collections.singletonList(resultA), actual);
    }

    @Test
    public void differentResultIsReportedAndActualResultReturned() {
        FilterWithShadowComparison sut = FilterWithShadowComparison.createForTesting(
                FilterStubs.delayedOK(0, resultA),
                FilterStubs.delayedOK(0, resultB),
                (a, b) -> differenceReportedSignal.set(true),
                comparisonTimeout100ms
        );

        List<FilterFeedback> actual = sut.filter(context);

        assertNoFailuresOccured();
        assertTrue(differenceReportedSignal.get());
        assertEquals(Collections.singletonList(resultA), actual);
    }

    @Test
    public void failureOfCanonicalFilterIsPropagated() {
        FilterWithShadowComparison sut = FilterWithShadowComparison.createForTesting(
                FilterStubs.throwingRTE(50),
                FilterStubs.delayedOK(0, resultA),
                failWhenDifferenceIsReported,
                Duration.ofMillis(100)
        );

        try {
            List<FilterFeedback> actual = sut.filter(context);
            fail("Shouldn't get here");
        } catch (RuntimeException e) {
            assertEquals(FilterStubs.simulatedFilterException, e);
        }
        assertNoFailuresOccured();
    }

    interface FilterStubs {
        RuntimeException simulatedFilterException = new RuntimeException("simulated by the test");

        static InterruptibleFilter delayedOK(long delayMillis, FilterFeedback feedback) {
            return context -> {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    throw InterruptibleFilter.createInterruptedException();
                }
                return Collections.singletonList(feedback);
            };
        }

        static InterruptibleFilter throwingRTE(long delayMillis) {
            return context -> {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    throw InterruptibleFilter.createInterruptedException();
                }
                throw simulatedFilterException;
            };
        }
    }

}