package com.ecg.replyts.core.runtime.retry;

/**
 * Lambda for wrapping a task that should be executed with retries
 *
 * @param <T> - return type from actual action
 */
@FunctionalInterface
public interface RetriableAction<T> {

    T act() throws Exception;
}
