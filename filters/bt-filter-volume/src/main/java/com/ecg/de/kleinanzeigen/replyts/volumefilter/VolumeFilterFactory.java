package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VolumeFilterFactory implements FilterFactory {
    @Autowired
    private SharedBrain sharedBrain;

    public Filter createPlugin(String filterName, JsonNode jsonNode) {
        ConfigurationParser config = new ConfigurationParser(jsonNode);

        return new VolumeFilter(filterName, sharedBrain,
          config.getQuotas(),
          config.isIgnoreFollowUps(),
          config.getExceptCategoriesList(),
          config.getAllowedCategoriesList());
    }
}