package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;

import javax.annotation.Nonnull;

public class VolumeFilterFactory implements FilterFactory {

    private final HazelcastInstance hazelcastInstance;
    private final EventStreamProcessor eventStreamProcessor;

    public VolumeFilterFactory(HazelcastInstance hazelcastInstance, EventStreamProcessor eventStreamProcessor) {
        this.hazelcastInstance = hazelcastInstance;
        this.eventStreamProcessor = eventStreamProcessor;
    }

    @Nonnull
    @Override
    public Filter createPlugin(String instanceId, JsonNode jsonConfiguration) {
        ConfigurationParser configuration = new ConfigurationParser(jsonConfiguration);
        SharedBrain sharedBrain = new SharedBrain(hazelcastInstance, eventStreamProcessor);

        eventStreamProcessor.register(instanceId, configuration.get());

        return new VolumeFilter(sharedBrain, configuration.get(), eventStreamProcessor, instanceId);
    }
}
