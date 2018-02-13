package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RetryCommandTest {
    private static final int MAXRETRIES = 3;

    RetryCommand<String> retryCommand;

    @Before
    public void setUp() throws Exception {
        retryCommand = new RetryCommand<>(MAXRETRIES);
    }

/*    @Test
    public void success_noRetries() throws Exception {
        String result = retryCommand.run(() -> "ok");
        assertEquals("ok", result);
        assertEquals(0, retryCommand.getRetryCount());
    }

    @Test
    public void oneFailure_oneRetry() throws Exception {
        String result = retryCommand.run(() -> {
            if (retryCommand.getRetryCount() == 0) {
                throw new RuntimeException("Failure");
            } else {
                return "ok";
            }
        });

        assertEquals("ok", result);
        assertEquals(1, retryCommand.getRetryCount());
    }

    @Test(expected = RuntimeException.class)
    public void allFailed_exceptionThrown() throws Exception {
        retryCommand.run(() -> {
            throw new RuntimeException("Failure");
        });
    }
*/}
