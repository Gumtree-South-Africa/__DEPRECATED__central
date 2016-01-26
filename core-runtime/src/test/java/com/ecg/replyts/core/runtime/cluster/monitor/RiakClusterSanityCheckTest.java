package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.ecg.replyts.core.runtime.Sleeper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RiakClusterSanityCheckTest {

    @Mock
    private RiakClusterHealthCheck healthCheck;

    @Mock
    private Sleeper sleeper;

    private RiakClusterSanityCheck check;

    @Before
    public void setUp() {
        check = new RiakClusterSanityCheck(healthCheck, 3, sleeper);
    }

    @Test
    public void retryIfFailed() throws InterruptedException {

        when(healthCheck.check()).thenReturn(CheckResult.createNonHealthyEmpty());

        Result result = check.execute();

        assertThat(result.status()).isEqualTo(Status.CRITICAL);
        verify(sleeper).sleep(TimeUnit.SECONDS, 2);
        verify(sleeper).sleep(TimeUnit.SECONDS, 4);
        verify(healthCheck, times(3)).check();
    }

    @Test
    public void shouldReportWithoutRetryIfOk() throws InterruptedException {

        when(healthCheck.check()).thenReturn(CheckResult.createHealthyEmpty());

        Result result = check.execute();

        assertThat(result.status()).isEqualTo(Status.OK);
        verify(sleeper, never()).sleep(TimeUnit.SECONDS, 2);
        verify(healthCheck, times(1)).check();
    }
}
