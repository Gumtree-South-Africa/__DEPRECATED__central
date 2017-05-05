package com.gumtree.comaas.filter.knowngood;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeKnownGoodFilterConfiguration {
    @Autowired
    private GumshieldApi gumshieldApi;

    @Bean
    public FilterFactory filterFactory() {
        return (instanceName, configuration) -> {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            Filter pluginConfig = new Filter(pluginFactory, instanceId, configurationNode);
            KnownGoodFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.textValue(), KnownGoodFilterConfig.class);

            return new GumtreeKnownGoodFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig).withUserApi(gumshieldApi.userApi());
        };
    }
}
