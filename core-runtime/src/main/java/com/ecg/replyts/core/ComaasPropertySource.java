package com.ecg.replyts.core;

import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ComaasPropertySource extends MapPropertySource {
    static final String NAME = "comaas_environment";

    public ComaasPropertySource() {
        this(NAME, new HashMap<String, Object>() {{
            Optional.ofNullable(System.getenv("COMAAS_HTTP_PORT")).ifPresent(port -> put("replyts.http.port", port));
            Optional.ofNullable(System.getenv("COMAAS_HAZELCAST_PORT")).ifPresent(port -> put("hazelcast.port", port));

            // XXX: Once everyone is over to the ecg-salt-comaas PR #108 we can s/replyts\.tenant/tenant/g

            Optional.of(System.getProperty("tenant")).ifPresent(tenant -> put("replyts.tenant", tenant));
        }});
    }

    private ComaasPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }
}
