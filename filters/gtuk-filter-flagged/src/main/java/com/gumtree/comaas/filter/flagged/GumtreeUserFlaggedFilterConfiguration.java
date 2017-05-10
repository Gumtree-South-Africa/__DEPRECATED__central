package com.gumtree.comaas.filter.flagged;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.config.UserFlaggedFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeUserFlaggedFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new UserFlaggedFilterFactory();
    }

    public static class UserFlaggedFilterFactory implements FilterFactory {
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            UserFlaggedFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), UserFlaggedFilterConfig.class);

            return new GumtreeUserFlaggedFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig);
        }
    }
}
