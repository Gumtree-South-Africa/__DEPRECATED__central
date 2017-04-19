package com.gumtree.comaas.filter.geoip;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.GeoIpFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeGeoIpFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return (instanceName, configuration) -> {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            Filter pluginConfig = new Filter(pluginFactory, instanceId, configurationNode);
            GeoIpFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), GeoIpFilterConfig.class);

            return new GumtreeGeoIpFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig);
        };
    }
}
