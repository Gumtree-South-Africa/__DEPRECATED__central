package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VolumeFilterFactory implements FilterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilterFactory.class);

    private final EventStreamProcessor eventStreamProcessor;
    private final SharedBrain sharedBrain;

    public VolumeFilterFactory(SharedBrain sharedBrain, EventStreamProcessor eventStreamProcessor) {
        this.sharedBrain = sharedBrain;
        this.eventStreamProcessor = eventStreamProcessor;
    }

    @Nonnull
    @Override
    public Filter createPlugin(String instanceId, JsonNode jsonConfiguration) {
        ConfigurationParser configuration = new ConfigurationParser(jsonConfiguration);

        Set<Window> uniqueWindows = new HashSet<>();
        for (Quota quota : configuration.get()) {
            Window window = new Window(instanceId, quota);
            if (uniqueWindows.contains(window)) {
                LOG.warn("window already exists '{}'", window.getWindowName());
            } else {
                uniqueWindows.add(window);
            }
        }

        LOG.info("Registering VolumeFilter with windows {}", uniqueWindows);
        eventStreamProcessor.register(uniqueWindows);

        return new VolumeFilter(sharedBrain, eventStreamProcessor, uniqueWindows);
    }
}
