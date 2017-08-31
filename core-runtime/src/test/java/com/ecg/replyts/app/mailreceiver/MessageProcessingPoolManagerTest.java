package com.ecg.replyts.app.mailreceiver;

import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MessageProcessingPoolManagerTest {

    @Test
    public void uncaughtExceptionInWorkerResultsInThreadRestart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Map<String, Integer> threadNames = new ConcurrentHashMap<>();

        MessageProcessingPoolManager manager = new MessageProcessingPoolManager(1, 1, () -> {
            threadNames.compute(Thread.currentThread().getName(), (k, v) -> v == null ? 1 : v + 1);
            latch.countDown();
            if (latch.getCount() == 1) {
                throw new RuntimeException("this one supposed to kill a worker thread so that it's resurrects");
            }
        });
        manager.startProcessing();

        assertTrue("the processing service didn't reduce the latch to 0: the processing thread hasn't been restarted",
                latch.await(10L, TimeUnit.SECONDS));

        // ensure that the thread has been restarted under the same name
        assertThat(threadNames.size(), is(1));
        assertThat(threadNames.values().iterator().next(), greaterThan(2));
        manager.stopProcessing();
    }

    @Test
    public void exactlyRequestedAmountOfThreadsIsStartedAndStopped() throws InterruptedException {
        Semaphore semaphore = new Semaphore(1, true);
        AtomicInteger workerInvocationCount = new AtomicInteger(0);
        semaphore.tryAcquire(1, TimeUnit.SECONDS);

        MessageProcessingPoolManager manager = new MessageProcessingPoolManager(5, 1, () -> {
            workerInvocationCount.incrementAndGet();
            semaphore.acquire();
        });
        manager.startProcessing();

        // Wait until at least 5 threads has been started and waiting for the semaphore
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(5L, TimeUnit.MILLISECONDS)
                .until(() -> semaphore.getQueueLength() >= 5);
        // Check that exactly 5 threads has been started
        assertThat(workerInvocationCount.get(), is(5));
        manager.stopProcessing();
    }

    @Test
    public void threadsAreConfiguredProperly() throws InterruptedException {
        Set<String> threadNames = new ConcurrentSkipListSet<>();
        Set<Boolean> daemonFlags = new ConcurrentSkipListSet<>();
        Semaphore semaphore = new Semaphore(1, true);
        semaphore.tryAcquire(1, TimeUnit.SECONDS);
        MessageProcessingPoolManager manager = new MessageProcessingPoolManager(5, 1, () -> {
            threadNames.add(Thread.currentThread().getName());
            daemonFlags.add(Thread.currentThread().isDaemon());
            semaphore.acquire();
        });
        manager.startProcessing();
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(5L, TimeUnit.MILLISECONDS)
                .until(() -> semaphore.getQueueLength() >= 5);
        // ensure that thread names are unique
        assertThat(threadNames.size(), is(5));
        // ensure that all threads are non-daemons
        assertThat(daemonFlags.size(), is(1));
        assertThat(daemonFlags.iterator().next(), is(false));
        manager.stopProcessing();
    }
}