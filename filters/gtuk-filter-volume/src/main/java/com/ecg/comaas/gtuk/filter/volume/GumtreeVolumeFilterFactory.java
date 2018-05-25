package com.ecg.comaas.gtuk.filter.volume;

import com.ecg.comaas.gtuk.filter.volume.monitoring.VolumeFilterMonitoringConfiguration;
import com.ecg.gumtree.comaas.common.domain.VelocityFilterConfig;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.search.SearchService;
import com.gumtree.common.util.time.SystemClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
@Component
@Import({ EventStreamProcessor.class, SharedBrain.class, VolumeFilterMonitoringConfiguration.class })
public class GumtreeVolumeFilterFactory extends GumtreeFilterFactory<VelocityFilterConfig, GumtreeVolumeFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.volume.GumtreeVolumeFilterConfiguration$VolumeFilterFactory";

    @Autowired
    public GumtreeVolumeFilterFactory(SearchService searchService, EventStreamProcessor eventStreamProcessor, SharedBrain sharedBrain) {
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

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}