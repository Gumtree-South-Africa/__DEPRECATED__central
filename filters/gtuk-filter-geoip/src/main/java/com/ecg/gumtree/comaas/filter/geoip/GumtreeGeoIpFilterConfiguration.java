package com.ecg.gumtree.comaas.filter.geoip;

import com.ecg.gumtree.comaas.common.filter.DisabledFilter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.config.GeoIpFilterConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeGeoIpFilterConfiguration.GeoIpFilterFactory.class)
public class GumtreeGeoIpFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new GeoIpFilterFactory();
    }

    public static class GeoIpFilterFactory implements FilterFactory {
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            GeoIpFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), GeoIpFilterConfig.class);

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            return new GumtreeGeoIpFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig);
        }
    }
}
