package com.ecg.de.ebayk.messagecenter.pushmessage.send.discovery;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.Objects;

final class ServiceLookupMetrics {
    private static final String PREFIX = "consul_catalog.lookup_service";

    private final MetricRegistry metricRegistry;

    ServiceLookupMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = Objects.requireNonNull(metricRegistry);
    }

    void notFound(ServiceName serviceName) {
        metricRegistry.meter(MetricRegistry.name(PREFIX, serviceName.asServiceName(), "notFound")).mark();
    }

    void found(ServiceName serviceName) {
        metricRegistry.meter(MetricRegistry.name(PREFIX, serviceName.asServiceName(), "found")).mark();
    }

    void failed(ServiceName serviceName) {
        metricRegistry.meter(MetricRegistry.name(PREFIX, serviceName.asServiceName(), "failed")).mark();
    }

    Timer.Context timer() {
        return this.metricRegistry.timer(PREFIX).time();
    }
}
