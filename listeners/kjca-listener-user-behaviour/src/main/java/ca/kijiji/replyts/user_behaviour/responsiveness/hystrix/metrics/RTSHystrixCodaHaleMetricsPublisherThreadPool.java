package ca.kijiji.replyts.user_behaviour.responsiveness.hystrix.metrics;

import com.codahale.metrics.MetricRegistry;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.contrib.codahalemetricspublisher.HystrixCodaHaleMetricsPublisherThreadPool;

class RTSHystrixCodaHaleMetricsPublisherThreadPool extends HystrixCodaHaleMetricsPublisherThreadPool {

    RTSHystrixCodaHaleMetricsPublisherThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics, HystrixThreadPoolProperties properties, MetricRegistry metricRegistry) {
        super(threadPoolKey, metrics, properties, metricRegistry);
    }

    @Override
    protected String createMetricName(String name) {
        return RTSHystrixCodaHaleMetricsPublisher.metricName(super.createMetricName(name));
    }
}
