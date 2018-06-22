package com.ecg.comaas.core.filter.remote;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class RemoteFilterFactory implements FilterFactory {
    @Nonnull
    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        Configuration c = Configuration.parse(configuration);
        return new RemoteFilter(instanceName, c.endpoint, c.filterTimeout);
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        return "core.remote";
    }

    static class Configuration {
        final String endpoint;
        final Duration filterTimeout;

        Configuration(String endpoint, Duration filterTimeout) {
            this.endpoint = checkNotNull(endpoint, "endpoint");
            this.filterTimeout = checkNotNull(filterTimeout, "filterTimeout");
        }

        static Configuration parse(JsonNode c) {
            JsonNode transportNode = c.get("transport");
            String endpoint = transportNode.get("endpoint").asText();
            Duration filterTimeout = Duration.parse(transportNode.get("filterTimeout").asText());
            return new Configuration(endpoint, filterTimeout);
        }
    }
}
