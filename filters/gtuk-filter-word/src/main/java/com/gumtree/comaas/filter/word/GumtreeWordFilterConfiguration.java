package com.gumtree.comaas.filter.word;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.comaas.common.filter.DisabledFilter;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeWordFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new WordFilterFactory();
    }

    public static class WordFilterFactory implements FilterFactory {
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            WordFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), WordFilterConfig.class);

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            return new GumtreeWordFilter(pluginConfig, filterConfig);
        }
    }
}
