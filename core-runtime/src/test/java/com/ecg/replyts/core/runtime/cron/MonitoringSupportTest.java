package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringSupportTest {
    @Mock
    private HazelcastInstance hazelcast;

    @InjectMocks
    private MonitoringSupport monitoringSupport;

    private final AtomicReferenceSimulator isRunningReferenceA = new AtomicReferenceSimulator();
    private final AtomicReferenceSimulator lastExceptionReferenceA = new AtomicReferenceSimulator();
    private final AtomicReferenceSimulator lastRunReferenceA = new AtomicReferenceSimulator();

    private final AtomicReferenceSimulator isRunningReferenceB = new AtomicReferenceSimulator();
    private final AtomicReferenceSimulator lastExceptionReferenceB = new AtomicReferenceSimulator();
    private final AtomicReferenceSimulator lastRunReferenceB = new AtomicReferenceSimulator();

    private static class CronJobExecutorA extends SampleCronJobExecutor {
        private CronJobExecutorA() {
            super("0 0 0 1 1 ? 2099");
        }
    }

    private static class CronJobExecutorB extends SampleCronJobExecutor {
        private CronJobExecutorB() {
            super("0 0 0 1 1 ? 2099");
        }
    }

    private final CronJobExecutorA a = new CronJobExecutorA();
    private final CronJobExecutorB b = new CronJobExecutorB();

    @Before
    public void setup() {
        when(hazelcast.getAtomicReference("CronJobExecutorA-isRunning")).thenReturn(isRunningReferenceA);
        when(hazelcast.getAtomicReference("CronJobExecutorA-lastException")).thenReturn(lastExceptionReferenceA);
        when(hazelcast.getAtomicReference("CronJobExecutorA-lastRun")).thenReturn(lastRunReferenceA);

        when(hazelcast.getAtomicReference("CronJobExecutorB-isRunning")).thenReturn(isRunningReferenceB);
        when(hazelcast.getAtomicReference("CronJobExecutorB-lastException")).thenReturn(lastExceptionReferenceB);
        when(hazelcast.getAtomicReference("CronJobExecutorB-lastRun")).thenReturn(lastRunReferenceB);

        ReflectionTestUtils.setField(monitoringSupport, "executors", Arrays.asList(a, b));
        ReflectionTestUtils.invokeMethod(monitoringSupport, "initialize");
    }

    @Test
    public void createsMultipleMonitoringBeans() {
        assertThat(monitoringSupport.getChecks()).hasSize(2);
    }

    @Test
    public void checkForServiceFails() throws Exception {
        monitoringSupport.failure(CronJobExecutorA.class, new IllegalStateException());

        assertThat(countFailedChecks()).isEqualTo(1);
    }

    @Test
    public void checkForServiceRecovers() throws Exception {

        monitoringSupport.failure(CronJobExecutorA.class, new IllegalStateException());
        monitoringSupport.success(CronJobExecutorA.class);

        assertThat(countFailedChecks()).isEqualTo(0);
    }


    private int countFailedChecks() throws Exception {
        int i = 0;
        for (Check check : monitoringSupport.getChecks()) {
            if (check.execute().status() == Status.CRITICAL) {
                i++;
            }
        }
        return i;
    }
}
