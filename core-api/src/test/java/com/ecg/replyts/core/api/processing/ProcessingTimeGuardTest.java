package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.util.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProcessingTimeGuardTest {
    public static final long ONE_SECOND = 1L;

    private final Date date = new Date(1000L);

    @Mock
    private Clock clock;

    @Test
    public void noExceptionIfProcessingTimeNotExceeded() {
        when(clock.now()).thenReturn(new Date(1999L));

        new ProcessingTimeGuard(date, ONE_SECOND, clock).check();
        // no exception expected here
    }

    @Test(expected = ProcessingTimeExceededException.class)
    public void exceptionIfProcessingTimeExceeded() {
        when(clock.now()).thenReturn(new Date(2001L));

        new ProcessingTimeGuard(date, ONE_SECOND, clock).check();
    }

    @Test
    public void zeroIsInfiniteProcessing() {
        new ProcessingTimeGuard(date, 0L, clock).check();
    }
}
