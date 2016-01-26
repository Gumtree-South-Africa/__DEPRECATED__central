package com.ecg.replyts.core.runtime.cron;

import com.hazelcast.core.IAtomicReference;
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
    public Object getId() {
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
