package com.ecg.replyts.core.runtime.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for performing retries of a {@link RetriableAction} if that action fails.
 * - if action succeeded, return action result
 * - if action fails, retry till specified amount of tries reached
 * - if action fails and no more allowed retries left, throw a {@link RetryException}
 */
public final class RetryService {

    private static final Logger LOG = LoggerFactory.getLogger(RetryService.class);

    private static final int MIN_TRIES_ALLOWED = 1;
    private static final int MAX_TRIES_ALLOWED = 100;

    private RetryService() {
    }

    /**
     * Silently executes {@link RetriableAction} - no exception would be thrown, null would be returned in case of failure
     *
     * @param retriableAction - lambda expression for performing an action
     * @param tries           - number of allowed executes
     * @param <T>             - return value from action
     * @return - action result if succeeded, otherwise null
     */
    public static <T> T executeSilently(RetriableAction<T> retriableAction, int tries) {
        try {
            return execute(retriableAction, tries);
        } catch (RetryException e) {
            LOG.warn("Returning 'null' as all retries of action failed", e);
            return null;
        }
    }

    /**
     * Executes {@link RetriableAction}, throws exception if all allowed executions failed
     *
     * @param retriableAction - lambda expression for performing an action
     * @param tries           - number of allowed executes
     * @param <T>             - return value from action
     * @return - action result if succeeded, otherwise throws exception
     * @throws RetryException - thrown if all allowed executions failed (action never succeeded)
     */
    public static <T> T execute(RetriableAction<T> retriableAction, int tries) throws RetryException {
        if (retriableAction == null) {
            throw new IllegalArgumentException("Retriable action can't be null");
        }

        if (tries < MIN_TRIES_ALLOWED || tries > MAX_TRIES_ALLOWED) {
            throw new IllegalArgumentException("Allowed between " + MIN_TRIES_ALLOWED + " and " + MAX_TRIES_ALLOWED + " tries inclusive");
        }

        int triesLeft = tries;
        Exception lastException = null;
        while (triesLeft > 0) {
            try {
                return retriableAction.act();
            } catch (Exception e) {
                --triesLeft;
                lastException = e;
                LOG.warn("Retriable action failed, retries left " + triesLeft, e);
            }
        }

        throw new RetryException("All " + tries + " retries of retriable action failed", lastException);
    }
}
