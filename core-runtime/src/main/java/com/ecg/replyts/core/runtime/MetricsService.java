package com.ecg.replyts.core.runtime;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Wrap the implementations of metric lib. Currently we use codahale metric plugin.
 * Export registry to reuse in external plugins.
 * <p/>
 * User: acharton
 * Date: 30.04.13
 */
public class MetricsService { // NOSONAR - this class is mocked

    /* centralized service used via get Instance. */
    private static final MetricsService INSTANCE = new MetricsService();

    private final MetricRegistry registry;

    private MetricsService() {
        this.registry = new MetricRegistry();
    }

    /**
     * Use for external plugins to access registry, like create own reporters.
     */
    public MetricRegistry getRegistry() {
        return registry;
    }

    public static MetricsService getInstance() {
        return INSTANCE;
    }

    public Timer timer(String name) {
        return registry.timer(name);
    }

    // returns counter from map (or init lazy)
    public Counter counter(String messageStateKey) {
        return registry.counter(messageStateKey);
    }

    public Histogram histogram(String name) {
        return registry.histogram(name);
    }
}
