package com.ecg.replyts.core.webapi;

import com.ecg.replyts.core.runtime.TimingReports;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import org.eclipse.jetty.util.annotation.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * HostReportingInstrumentedQueuedTPool implementation that reports hostname as part of metrics name
 * most of the implementation is copied from https://github.com/dropwizard/metrics/blob/3.2-development/metrics-jetty9/src/main/java/com/codahale/metrics/jetty9/InstrumentedQueuedThreadPool.java
 */
public class HostReportingInstrumentedQueuedTPool extends QueuedThreadPool {

    private final MetricRegistry metricRegistry;

    public HostReportingInstrumentedQueuedTPool(@Name("registry") MetricRegistry registry) {
        this(registry, 200);
    }

    public HostReportingInstrumentedQueuedTPool(@Name("registry") MetricRegistry registry,
                                                @Name("maxThreads") int maxThreads) {
        this(registry, maxThreads, 8);
    }

    public HostReportingInstrumentedQueuedTPool(@Name("registry") MetricRegistry registry,
                                                @Name("maxThreads") int maxThreads,
                                                @Name("minThreads") int minThreads) {
        this(registry, maxThreads, minThreads, 60000);
    }

    public HostReportingInstrumentedQueuedTPool(@Name("registry") MetricRegistry registry,
                                                @Name("maxThreads") int maxThreads,
                                                @Name("minThreads") int minThreads,
                                                @Name("idleTimeout") int idleTimeout) {
        this(registry, maxThreads, minThreads, idleTimeout, null);
    }

    public HostReportingInstrumentedQueuedTPool(@Name("registry") MetricRegistry registry,
                                                @Name("maxThreads") int maxThreads,
                                                @Name("minThreads") int minThreads,
                                                @Name("idleTimeout") int idleTimeout,
                                                @Name("queue") BlockingQueue<Runnable> queue) {
        super(maxThreads, minThreads, idleTimeout, queue);
        this.metricRegistry = registry;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        String threadPoolName = QueuedThreadPool.class.getCanonicalName();
        String hostName = TimingReports.getHostName();
        metricRegistry.register(name(hostName, threadPoolName, getName(), "busy-threads"), (Gauge<Integer>) this::getBusyThreads);
        metricRegistry.register(name(hostName, threadPoolName, getName(), "utilization"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(getBusyThreads(), getThreads());
            }
        });
        metricRegistry.register(name(hostName, threadPoolName, getName(), "utilization-max"), new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return Ratio.of(getBusyThreads(), getMaxThreads());
            }
        });
        metricRegistry.register(name(hostName, threadPoolName, getName(), "size"), (Gauge<Integer>) this::getThreads);
        metricRegistry.register(name(hostName, threadPoolName, getName(), "jobs"), (Gauge<Integer>) () -> {
            // This assumes the QueuedThreadPool is using a BlockingArrayQueue or
            // ArrayBlockingQueue for its queue, and is therefore a constant-time operation.
            return getQueue().size();
        });
    }
}
