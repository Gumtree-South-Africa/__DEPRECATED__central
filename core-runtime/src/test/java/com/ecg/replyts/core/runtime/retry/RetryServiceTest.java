package com.ecg.replyts.core.runtime.retry;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryServiceTest {

    @Test(expected = IllegalArgumentException.class)
    public void executeSilently_whenActionIsNull_shouldThrowException() {
        RetryService.executeSilently(null, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void executeSilently_whenTriesAmountIncorrect_shouldThrowException() {
        RetryService.executeSilently(() -> null, -10);
        RetryService.executeSilently(() -> null, 1_000);
    }

    @Test(expected = RetryException.class)
    public void execute_whenAllTriesFailed_shouldThrowException() throws RetryException {
        RetryService.execute(() -> {
            throw new Exception();
        }, 2);
    }

    @Test
    public void executeSilently_whenAllTriesFailed_shouldReturnNull() {
        Object actual = RetryService.executeSilently(() -> {
            throw new Exception();
        }, 2);

        assertThat(actual).isNull();
    }

    @Test
    public void executeSilently_whenSucceededAfterFailure_shouldReturnValue() {
        AtomicInteger atomicCounter = new AtomicInteger();
        Boolean actual = RetryService.executeSilently(() -> {
            if (atomicCounter.get() > 2) {
                return true;
            } else {
                atomicCounter.incrementAndGet();
                throw new Exception();
            }
        }, 5);

        assertThat(actual).isTrue();
    }

    @Test
    public void executeSilently_whenSucceeded_shouldReturnValue() {
        String expected = "expectedValue";
        String actual = RetryService.executeSilently(() -> expected, 2);

        assertThat(actual).isEqualTo(expected);
    }
}
