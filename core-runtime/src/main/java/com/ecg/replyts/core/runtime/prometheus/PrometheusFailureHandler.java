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

    public static void reportExternalServiceFailure(ExternalServiceType externalServiceType) {
        EXTERNAL_SERVICE_FAILURE.labels(externalServiceType.getLabel()).inc();
    }
}
