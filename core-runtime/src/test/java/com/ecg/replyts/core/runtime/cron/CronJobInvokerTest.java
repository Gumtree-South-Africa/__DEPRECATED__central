package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CronJobInvokerTest {
    @Spy
    private SampleCronJobExecutor cje = new SampleCronJobExecutor("0 0 0 1 1 ? 2099");

    @Mock
    private HazelcastInstance hci;

    @Mock
    private ILock lock;

    @Mock
    private DistributedExecutionStatusMonitor monitor;

    private final AtomicReferenceSimulator atomicReference = new AtomicReferenceSimulator();

    private CronJobService srvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(hci.getAtomicReference(anyString())).thenReturn(atomicReference);

        String confDir;
        try {
            confDir = new ClassPathResource("conf").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String quartzConfig = confDir + "/quartz.properties";

        when(hci.getLock(Mockito.anyString())).thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);
        srvc = new CronJobService(true, Collections.<CronJobExecutor>singletonList(cje), monitor, hci, false);
    }

    @Test
    public void registersMBeans() throws Exception {
        new CronJobInvoker(cje, srvc).invoke();
        verify(cje, timeout(5000)).execute();
    }

}
