package com.ecg.replyts.core.runtime.prometheus;

import io.prometheus.client.Counter;

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
     * @param serviceFailureDescription Short description of the service and context that fails. This value is propagated 
     * to the oncall person. This should NOT contain any dynamic values (f.e. message id) as this will break Prometheus.
     */
    public static void reportExternalServiceFailure(String serviceFailureDescription) {
        EXTERNAL_SERVICE_FAILURE.labels(serviceFailureDescription).inc();
    }
}
