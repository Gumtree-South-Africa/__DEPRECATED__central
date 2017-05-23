package com.ecg.gumtree.comaas.filter.url;

import com.ecg.gumtree.comaas.common.filter.DisabledFilter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.UrlFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeUrlFilterConfiguration.UrlFilterFactory.class)
public class GumtreeUrlFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new UrlFilterFactory();
    }

    public static class UrlFilterFactory implements FilterFactory {
        @Override
        public com.ecg.replyts.core.api.pluginconfiguration.filter.Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            Filter pluginConfig = new Filter(pluginFactory, instanceId, configurationNode);
            UrlFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), UrlFilterConfig.class);

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            return new GumtreeUrlFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig);
        }
    }
}
