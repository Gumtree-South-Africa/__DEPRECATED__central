package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by reweber on 21/10/15
 */
public class KnownGoodFilterFactory implements FilterFactory {
    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        KnownGoodFilterConfig knownGoodFilterConfig = new KnownGoodFilterConfig();
        return new KnownGoodFilter(instanceName, knownGoodFilterConfig);
    }
}
