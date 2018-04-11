package com.ecg.comaas.gtau.filter.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
class VolumeFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.volumefilter.VolumeFilterFactory";

    private final SharedBrain sharedBrain;

    @Autowired
    public VolumeFilterFactory(HazelcastInstance hazelcastInstance) {
        this.sharedBrain = new SharedBrain(hazelcastInstance);
    }

    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        final ConfigurationParser parser = new ConfigurationParser(jsonNode);
        return new VolumeFilter(sharedBrain, parser.get(), parser.getWhitelistedEmails());
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
