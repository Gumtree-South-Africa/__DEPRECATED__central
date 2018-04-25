package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile(TENANT_GTAU)
@Component
public class EchelonFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ebay.replyts.australia.echelon.EchelonFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new EchelonFilter(EchelonFilterPatternRulesParser.getConfig(configuration));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
