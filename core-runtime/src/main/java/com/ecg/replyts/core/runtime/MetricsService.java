package com.ecg.replyts.core.runtime;

import com.codahale.metrics.*;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

import java.lang.management.ManagementFactory;
import java.util.Map;

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
        registerAll("jvm." + TimingReports.getHostName() + ".gc", new GarbageCollectorMetricSet(), this.registry);
        registerAll("jvm." + TimingReports.getHostName() + ".buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()), this.registry);
        registerAll("jvm." + TimingReports.getHostName() + ".memory", new MemoryUsageGaugeSet(), this.registry);
        registerAll("jvm." + TimingReports.getHostName() + ".threads", new ThreadStatesGaugeSet(), this.registry);
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

    private void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

}
