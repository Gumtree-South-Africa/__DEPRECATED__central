package com.ebay.replyts.australia.echelon;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author mdarapour
 */
public class EchelonFilterFactory implements FilterFactory {
    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new EchelonFilter(EchelonFilterPatternRulesParser.getConfig(configuration));
    }
}
