package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronExecution;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@Ignore
public class CronJobServiceIntegrationTest {
    @Mock
    private HazelcastInstance hci;

    @Mock
    private ILock lock;

    @Mock
    private DistributedExecutionStatusMonitor monitor;

    CountDownLatch cdl = new CountDownLatch(1);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(hci.getLock(Mockito.anyString())).thenReturn(lock);
        assertEquals(hci.getLock("foo"), lock);
        when(lock.tryLock()).thenReturn(true);
    }


    private CronJobExecutor cje = new SampleCronJobExecutor(CronExpressionBuilder.everyNSeconds(1)) {
        public void execute() throws Exception {
            cdl.countDown();
        }
    };

    @Test(timeout = 2000)
    public void registersAndFiresCronjob() throws Exception {
        new CronJobService(false, Arrays.asList(cje), monitor, hci, false);
        cdl.await();
    }

    @Test
    public void skipsJobExecutionWhenNotMaster() throws Exception {
        when(lock.tryLock()).thenReturn(false);
        CronJobService item = spy(new CronJobService(false, Arrays.asList(cje), monitor, hci, false));
        item.invokeMonitoredIfLeader(cje.getClass());
        verify(item, never()).invokeMonitored(any(Class.class));
        // assertFalse(cdl.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void notifiesStatusMonitor() throws InterruptedException {
        new CronJobService(false, Arrays.asList(cje), monitor, hci, false);
        cdl.await();
        InOrder inOrder = inOrder(monitor);
        inOrder.verify(monitor).start(any(CronExecution.class));
        inOrder.verify(monitor).end(any(CronExecution.class));
    }
}
