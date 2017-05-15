package com.gumtree.comaas.filter.watchlist;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeWatchlistFilterConfiguration {
    @Bean
    public FilterFactory filterFactory(GumshieldApi gumshieldApi) {
        return new WatchlistFilterFactory(gumshieldApi);
    }

    public static class WatchlistFilterFactory implements FilterFactory {
        private GumshieldApi gumshieldApi;

        WatchlistFilterFactory(GumshieldApi gumshieldApi) {
            this.gumshieldApi = gumshieldApi;
        }

        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            WatchlistFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), WatchlistFilterConfig.class);

            return new GumtreeWatchlistFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig).withChecklistApi(gumshieldApi.checklistApi());
        }
    }
}
