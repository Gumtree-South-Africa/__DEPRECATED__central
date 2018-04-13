package com.ecg.comaas.kjca.filter.volumefilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Tries to run a no-arg function. If it throws an Exception,
 * retries it the provided number of times. If it doesn't return
 * successfully when the retry limit is reached, a RuntimeException
 * is thrown.
 */
public class RetryCommand<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RetryCommand.class);

    private int retryCount = 0;
    private int maxRetries;

    public RetryCommand(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public T run(Supplier<T> function) {
        try {
            return function.get();
        } catch (Exception e) {
            LOG.warn("Command failed.", e);
            return retry(function);
        }
    }

    private T retry(Supplier<T> function) {
        LOG.warn("Command failed. Will retry {} times.", maxRetries);

        while (retryCount < maxRetries) {
            try {
                return function.get();
            } catch (Exception e) {
                retryCount++;
                LOG.warn("Command failed. Retry {} of {}.", retryCount, maxRetries, e);
                if (retryCount >= maxRetries) {
                    LOG.warn("Retry limit reached.");
                    break;
                }
            }
        }

        throw new RuntimeException("Command failed " + maxRetries + " retries");
    }

    // for testing
    int getRetryCount() {
        return retryCount;
    }
}
