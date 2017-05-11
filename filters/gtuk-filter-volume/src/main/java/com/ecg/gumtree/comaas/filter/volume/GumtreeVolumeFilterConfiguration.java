package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.gumtree.comaas.common.filter.DisabledFilter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.common.util.time.SystemClock;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import({EventStreamProcessor.class, SharedBrain.class})
public class GumtreeVolumeFilterConfiguration {
    @Bean
    public FilterFactory volumeFilterFactory(SearchService service, EventStreamProcessor eventStreamProcessor, SharedBrain sharedBrain) {
        return new VolumeFilterFactory(service, eventStreamProcessor, sharedBrain);
    }

    public static class VolumeFilterFactory implements FilterFactory {
        private SearchService searchService;
        private EventStreamProcessor eventStreamProcessor;
        private SharedBrain sharedBrain;

        VolumeFilterFactory(SearchService searchService, EventStreamProcessor eventStreamProcessor, SharedBrain sharedBrain) {
            this.searchService = searchService;
            this.eventStreamProcessor = eventStreamProcessor;
            this.sharedBrain = sharedBrain;
        }

        @Override
        public com.ecg.replyts.core.api.pluginconfiguration.filter.Filter createPlugin(String instanceId, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            Filter pluginConfig = new Filter(pluginFactory, instanceId, configurationNode);
            VelocityFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), VelocityFilterConfig.class);
            VolumeFilterServiceHelper volumeFilterServiceHelper = new VolumeFilterServiceHelper(new SystemClock());

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            eventStreamProcessor.register(instanceId, filterConfig);
            return new GumtreeVolumeFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig)
                    .withSearchService(searchService).withVolumeFilterServiceHelper(volumeFilterServiceHelper)
                    .withEventStreamProcessor(eventStreamProcessor).withInstanceName(instanceId).withSharedBrain(sharedBrain);
        }
    }
}
