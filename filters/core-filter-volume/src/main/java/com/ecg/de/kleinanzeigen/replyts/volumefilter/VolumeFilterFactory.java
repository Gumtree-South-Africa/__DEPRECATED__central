package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

public class VolumeFilterFactory implements FilterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilterFactory.class);

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

        List<Window> windows = Lists.transform(configuration.get(), q -> new Window(instanceId, q));

        LOG.info("Registering a new VolumeFilter's instance with windows {}", windows);
        eventStreamProcessor.register(windows);

        return new VolumeFilter(sharedBrain, eventStreamProcessor, windows);
    }
}
