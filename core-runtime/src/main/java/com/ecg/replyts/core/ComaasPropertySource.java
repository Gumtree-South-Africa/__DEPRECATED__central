package com.ecg.replyts.core;

import org.springframework.core.env.MapPropertySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ComaasPropertySource extends MapPropertySource {
    static final String NAME = "comaas_environment";

    private static Map<String, Object> getEnvVars() {
        Map<String, Object> properties = new HashMap<>();

        putEnvIfNotNull(properties, "COMAAS_HTTP_PORT", "replyts.http.port");
        putEnvIfNotNull(properties, "COMAAS_HAZELCAST_IP", "hazelcast.ip");
        putEnvIfNotNull(properties, "COMAAS_HAZELCAST_PORT", "hazelcast.port");
        putEnvIfNotNull(properties, "COMAAS_PROMETHEUS_PORT", "prometheus.port");
        putEnvIfNotNull(properties, "COMAAS_RUN_CRON_JOBS", "node.run.cronjobs");
        putEnvIfNotNull(properties, "MAIL_PROVIDER_STRATEGY", "mail.provider.strategy");

        if (System.getProperty("tenant") != null) {
            // XXX: Once everyone is over to the ecg-salt-comaas PR #108 we can s/replyts\.tenant/tenant/g
            properties.put("replyts.tenant", System.getProperty("tenant"));
        }

        return Collections.unmodifiableMap(properties);
    }

    private static void putEnvIfNotNull(Map<String, Object> builder, String envVar, String propertyName) {
        if (System.getenv(envVar) != null) {
            builder.put(propertyName, System.getenv(envVar));
        }
    }

    ComaasPropertySource() {
        this(NAME, getEnvVars());
    }

    private ComaasPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }
}
