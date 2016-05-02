package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import ca.kijiji.replyts.Activation;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory that generates volumefilters form a json config
 */
class VolumeFilterFactory implements FilterFactory {

    private final HazelcastInstance hazelcastInstance;

    @Autowired
    public VolumeFilterFactory(@SuppressWarnings("SpringJavaAutowiringInspection") HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        ConfigurationParser config = new ConfigurationParser(jsonNode);

        return new VolumeFilter(
                filtername,
                hazelcastInstance,
                config.getQuotas(),
                config.isIgnoreFollowUps(),
                new Activation(jsonNode)
        );
    }
}
