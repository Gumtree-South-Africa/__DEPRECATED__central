package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import ca.kijiji.replyts.Activation;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

/**
 * Factory that generates volume filters from a json config
 */
class VolumeFilterFactory implements FilterFactory {
    private static final String PROVIDER_NAME_PREFIX = "volumefilter_provider_";

    private final HazelcastInstance hazelcastInstance;

    @Autowired
    public VolumeFilterFactory(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public Filter createPlugin(String filterName, JsonNode configuration) {
        ConfigurationParser config = new ConfigurationParser(configuration);
        EventStreamProcessor eventStreamProcessor = new EventStreamProcessor(PROVIDER_NAME_PREFIX + filterName + "_" + new Random().nextInt(), config.getQuotas());

        return new VolumeFilter(
                filterName,
                new SharedBrain(filterName, hazelcastInstance, eventStreamProcessor),
                config.getQuotas(),
                config.isIgnoreFollowUps(),
                new Activation(configuration),
                eventStreamProcessor
        );
    }
}
