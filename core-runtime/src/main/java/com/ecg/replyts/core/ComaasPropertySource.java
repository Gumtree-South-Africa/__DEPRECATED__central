package com.ecg.replyts.core;

import com.google.common.collect.ImmutableMap;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class ComaasPropertySource extends MapPropertySource {
    static final String NAME = "comaas_environment";

    private static Map<String, Object> getEnvVars() {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        putEnvIfNotNull(builder, "COMAAS_HTTP_PORT", "replyts.http.port");
        putEnvIfNotNull(builder, "COMAAS_HAZELCAST_IP", "hazelcast.ip");
        putEnvIfNotNull(builder, "COMAAS_HAZELCAST_PORT", "hazelcast.port");

        if (System.getProperty("tenant") != null) {
            // XXX: Once everyone is over to the ecg-salt-comaas PR #108 we can s/replyts\.tenant/tenant/g
            builder.put("replyts.tenant", System.getProperty("tenant"));
        }

        return builder.build();
    }

    private static void putEnvIfNotNull(ImmutableMap.Builder<String, Object> builder, String envVar, String propertyName) {
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
