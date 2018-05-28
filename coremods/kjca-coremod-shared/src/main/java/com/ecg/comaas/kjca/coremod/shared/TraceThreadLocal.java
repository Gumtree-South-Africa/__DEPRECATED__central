package com.ecg.comaas.kjca.coremod.shared;

import org.slf4j.MDC;

/**
 * A thread-local that facilitates access to the current trace number. When using this thread local, it is extremely
 * important to call <code>remove</code>; failure to do so will result in a memory leak.
 * <p>
 * As a convenient side effect of this class, the current trace number will be bound into any supporting SLF4J logger's
 * map diagnostic context.
 */
public final class TraceThreadLocal {
    private static final ThreadLocal<String> TRACE_NUMBER = new ThreadLocal<String>();

    private TraceThreadLocal() {
    }

    /**
     * Set the current thread's trace number. Remember to call <code>remove</code> when you are done.
     * @param traceNumber
     */
    public static void set(final String traceNumber) {
        TRACE_NUMBER.set(traceNumber);
        MDC.put("trace", traceNumber);
    }

    /**
     * Get the current thread's trace number.
     */
    public static String get() {
        return TRACE_NUMBER.get();
    }

    /**
     * Remove all values of this thread local and free any reserved memory..
     */
    public static void reset() {
        MDC.remove("trace");
        TRACE_NUMBER.remove();
    }
}
