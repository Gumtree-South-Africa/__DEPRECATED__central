package com.gumtree.comaas.filter.word;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeWordFilterConfiguration {
    @Bean
    public FilterFactory wordFilterFactory() {
        return (instanceName, configuration) -> {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            Filter pluginConfig = new Filter(pluginFactory, instanceId, configurationNode);
            WordFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), WordFilterConfig.class);

            return new GumtreeWordFilter(pluginConfig, filterConfig);
        };
    }
}
