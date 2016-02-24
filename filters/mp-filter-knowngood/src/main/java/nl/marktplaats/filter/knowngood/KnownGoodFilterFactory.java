package nl.marktplaats.filter.knowngood;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class KnownGoodFilterFactory implements FilterFactory {
    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        KnownGoodFilterConfig knownGoodFilterConfig = new KnownGoodFilterConfig();
        return new KnownGoodFilter(instanceName, knownGoodFilterConfig);
    }
}
