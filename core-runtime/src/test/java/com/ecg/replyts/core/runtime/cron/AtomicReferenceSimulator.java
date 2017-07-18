package com.ecg.replyts.core.runtime.cron;

import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IFunction;

/**
 * Simulate a atomic reference from Hazelcast because it's simpler than mocking.
 */
class AtomicReferenceSimulator implements IAtomicReference<Object> {
    private Object value;

    @Override
    public boolean compareAndSet(Object expect, Object update) {
        return false;
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    public void set(Object newValue) {
        value = newValue;
    }

    @Override
    public Object getAndSet(Object newValue) {
        return null;
    }

    @Override
    public Object setAndGet(Object update) {
        return null;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean contains(Object value) {
        return false;
    }

    @Override
    public void alter(IFunction<Object, Object> function) {

    }

    @Override
    public Object alterAndGet(IFunction<Object, Object> function) {
        return null;
    }

    @Override
    public Object getAndAlter(IFunction<Object, Object> function) {
        return null;
    }

    @Override
    public <R> R apply(IFunction<Object, R> function) {
        return null;
    }

    @Override
    public ICompletableFuture<Boolean> compareAndSetAsync(Object expect, Object update) {
        return null;
    }

    @Override
    public ICompletableFuture<Object> getAsync() {
        return null;
    }

    @Override
    public ICompletableFuture<Void> setAsync(Object newValue) {
        return null;
    }

    @Override
    public ICompletableFuture<Object> getAndSetAsync(Object newValue) {
        return null;
    }

    @Override
    public ICompletableFuture<Boolean> isNullAsync() {
        return null;
    }

    @Override
    public ICompletableFuture<Void> clearAsync() {
        return null;
    }

    @Override
    public ICompletableFuture<Boolean> containsAsync(Object expected) {
        return null;
    }

    @Override
    public ICompletableFuture<Void> alterAsync(IFunction<Object, Object> function) {
        return null;
    }

    @Override
    public ICompletableFuture<Object> alterAndGetAsync(IFunction<Object, Object> function) {
        return null;
    }

    @Override
    public ICompletableFuture<Object> getAndAlterAsync(IFunction<Object, Object> function) {
        return null;
    }

    @Override
    public <R> ICompletableFuture<R> applyAsync(IFunction<Object, R> function) {
        return null;
    }

    @Override
    public String getPartitionKey() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
