package com.ecg.gumtree.comaas.filter.blacklist;

import com.ecg.gumtree.comaas.common.filter.DisabledFilter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.config.BlacklistFilterConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeBlacklistFilterConfiguration.BlacklistFilterFactory.class)
public class GumtreeBlacklistFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new BlacklistFilterFactory();
    }

    public static class BlacklistFilterFactory implements FilterFactory {
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            BlacklistFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), BlacklistFilterConfig.class);

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            return new GumtreeBlacklistFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig);
        }
    }
}
