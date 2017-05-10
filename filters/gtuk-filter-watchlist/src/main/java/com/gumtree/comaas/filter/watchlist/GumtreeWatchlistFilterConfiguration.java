package com.gumtree.comaas.filter.watchlist;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeWatchlistFilterConfiguration {
    @Autowired
    private GumshieldApi gumshieldApi;

    @Bean
    public FilterFactory filterFactory() {
        return (instanceName, configuration) -> {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            Filter pluginConfig = new Filter(pluginFactory, instanceId, configurationNode);
            WatchlistFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), WatchlistFilterConfig.class);

            return new GumtreeWatchlistFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig).withChecklistApi(gumshieldApi.checklistApi());
        };
    }
}
