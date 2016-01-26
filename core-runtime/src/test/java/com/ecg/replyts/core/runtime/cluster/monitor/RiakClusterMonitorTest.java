package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.sanitychecks.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Timer;
import java.util.TimerTask;

import static com.ecg.replyts.core.api.sanitychecks.Result.createResult;
import static com.ecg.replyts.core.api.sanitychecks.Status.CRITICAL;
import static com.ecg.replyts.core.api.sanitychecks.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakClusterMonitorTest {

    private static class MockTimer extends Timer {

        TimerTask task;

        @Override
        public void schedule(TimerTask task, long delay, long period) {
            this.task = task;
        }

    }

    @Mock
    private RiakClusterSanityCheck check;

    private final MockTimer timer = new MockTimer();

    private RiakClusterMonitor instance;

    @Before
    public void setUp() {
        instance = new RiakClusterMonitor(1000L, check, timer);
    }

    @After
    public void shutdown() {
        instance.shutdown();
    }

    @Test
    public void successfulCheckMarksDCsUp() throws InterruptedException {
        when(check.execute()).thenReturn(createResult(OK, Message.EMPTY));
        instance.init();
        timer.task.run();

        assertThat(instance.allDatacentersAvailable(), is(true));
    }

    @Test
    public void failedCheckMarksDCsDown() throws InterruptedException {
        when(check.execute()).thenReturn(createResult(CRITICAL, Message.EMPTY));
        instance.init();
        timer.task.run();

        assertThat(instance.allDatacentersAvailable(), is(false));
    }

    @Test
    public void exceptionDuringCheckMarksDCsDown() throws InterruptedException {
        when(check.execute()).thenThrow(new IllegalStateException("No connection to node"));
        instance.init();
        timer.task.run();

        assertThat(instance.allDatacentersAvailable(), is(false));
    }
}
