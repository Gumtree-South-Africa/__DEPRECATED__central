package com.ecg.comaas.bt.filter.volume;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VolumeFilterFactory implements FilterFactory {
    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.volumefilter.VolumeFilterFactory";

    @Autowired
    private SharedBrain sharedBrain;

    @Override
    public Filter createPlugin(String filterName, JsonNode jsonNode) {
        ConfigurationParser config = new ConfigurationParser(jsonNode);

        return new VolumeFilter(filterName, sharedBrain,
          config.getQuotas(),
          config.isIgnoreFollowUps(),
          config.getExceptCategoriesList(),
          config.getAllowedCategoriesList());
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}