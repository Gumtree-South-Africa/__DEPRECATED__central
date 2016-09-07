package ca.kijiji.replyts.user_behaviour.responsiveness.hystrix.metrics;

import com.codahale.metrics.MetricRegistry;
import com.netflix.hystrix.*;
import com.netflix.hystrix.contrib.codahalemetricspublisher.HystrixCodaHaleMetricsPublisherCommand;

class RTSHystrixCodaHaleMetricsPublisherCommand extends HystrixCodaHaleMetricsPublisherCommand {
    RTSHystrixCodaHaleMetricsPublisherCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties, MetricRegistry metricRegistry) {
        super(commandKey, commandGroupKey, metrics, circuitBreaker, properties, metricRegistry);
    }

    @Override
    protected String createMetricName(String name) {
        return RTSHystrixCodaHaleMetricsPublisher.metricName(super.createMetricName(name));
    }
}
