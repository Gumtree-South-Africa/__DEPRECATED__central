package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;

/** Factory that generates volumefilters form a json config */
class VolumeFilterFactory implements FilterFactory {
    private final SharedBrain sharedBrain;


    @Autowired
    public VolumeFilterFactory(HazelcastInstance hazelcastInstance) {
        this.sharedBrain = new SharedBrain(hazelcastInstance);
    }

    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        return new VolumeFilter(sharedBrain, new ConfigurationParser(jsonNode).get());
    }
}
