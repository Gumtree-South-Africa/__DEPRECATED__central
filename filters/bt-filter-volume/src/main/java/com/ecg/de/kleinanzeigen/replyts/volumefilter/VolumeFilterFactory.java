package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class VolumeFilterFactory implements FilterFactory {
    @Autowired
    private HazelcastInstance hazelcastInstance;

    public Filter createPlugin(String filterName, JsonNode jsonNode) {
        ConfigurationParser config = new ConfigurationParser(jsonNode);

        return new VolumeFilter(filterName, hazelcastInstance,
          config.getQuotas(),
          config.isIgnoreFollowUps(),
          config.getExceptCategoriesList(),
          config.getAllowedCategoriesList());
    }
}
