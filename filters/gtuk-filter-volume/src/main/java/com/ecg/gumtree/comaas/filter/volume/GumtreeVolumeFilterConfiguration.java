package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.common.util.time.SystemClock;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
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

    public static class VolumeFilterFactory extends GumtreeFilterFactory<VelocityFilterConfig, GumtreeVolumeFilter> {
        VolumeFilterFactory(SearchService searchService, EventStreamProcessor eventStreamProcessor, SharedBrain sharedBrain) {
            super(VelocityFilterConfig.class, (a, b) -> new GumtreeVolumeFilter()
                            .withPluginConfig(a)
                            .withFilterConfig(b)
                            .withSearchService(searchService)
                            .withVolumeFilterServiceHelper(new VolumeFilterServiceHelper(new SystemClock()))
                            .withEventStreamProcessor(eventStreamProcessor)
                            .withInstanceName(a.getInstanceId()).withSharedBrain(sharedBrain),
                    eventStreamProcessor::register
            );
        }
    }
}
