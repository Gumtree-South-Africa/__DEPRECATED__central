package com.gumtree.comaas.filter.knowngood;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.comaas.common.filter.DisabledFilter;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.json.ConfigMapper;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeKnownGoodFilterConfiguration {
    @Bean
    public FilterFactory filterFactory(GumshieldApi gumshieldApi) {
        return new KnownGoodFilterFactory(gumshieldApi);
    }

    public static class KnownGoodFilterFactory implements FilterFactory {
        private GumshieldApi gumshieldApi;

        KnownGoodFilterFactory(GumshieldApi gumshieldApi) {
            this.gumshieldApi = gumshieldApi;
        }

        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            KnownGoodFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.textValue(), KnownGoodFilterConfig.class);

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            return new GumtreeKnownGoodFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig).withUserApi(gumshieldApi.userApi());
        }
    }
}
