package com.ecg.replyts.core.runtime.prometheus;

import io.prometheus.client.Counter;

/**
 * Reports failures to Prometheus.
 * Values in methods are propagated to the oncall person.
 * This should NOT contain any dynamic values (f.e. message id) as this will break Prometheus.
 */
public final class PrometheusFailureHandler {

    private static final Counter EXTERNAL_SERVICE_FAILURE = Counter.build()
            .name("external_service_failure")
            .help("Tracks external services failures")
            .labelNames("external_service")
            .register();

    private PrometheusFailureHandler() {
    }

    /**
     * Report an external service failure
     *
     * @param serviceFailureDescription - short description of the service and context that fails.
     */
    public static void reportExternalServiceFailure(String serviceFailureDescription) {
        EXTERNAL_SERVICE_FAILURE.labels(serviceFailureDescription).inc();
    }
}
