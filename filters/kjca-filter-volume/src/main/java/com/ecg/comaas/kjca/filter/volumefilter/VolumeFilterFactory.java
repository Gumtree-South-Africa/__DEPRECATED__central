package com.ecg.comaas.kjca.filter.volumefilter;

import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

@Component
public class VolumeFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.volumefilter.VolumeFilterFactory";

    private static final AtomicInteger EVENT_PROCESSOR_COUNTER = new AtomicInteger(0);

    private static final String PROVIDER_NAME_PREFIX = "volumefilter_provider_";

    @Autowired
    private SharedBrain sharedBrain;

    @Override
    public Filter createPlugin(String filterName, JsonNode configuration) {
        ConfigurationParser config = new ConfigurationParser(configuration);

        EventStreamProcessor eventStreamProcessor = new EventStreamProcessor(format("%s%s_%d", PROVIDER_NAME_PREFIX, filterName, EVENT_PROCESSOR_COUNTER.getAndIncrement()), config.getQuotas());

        return new VolumeFilter(
          filterName,
          sharedBrain,
          config.getQuotas(),
          config.isIgnoreFollowUps(),
          new Activation(configuration),
          eventStreamProcessor);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
