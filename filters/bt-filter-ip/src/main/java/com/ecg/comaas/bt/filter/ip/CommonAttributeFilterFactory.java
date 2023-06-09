package com.ecg.comaas.bt.filter.ip;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_AR;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MX;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_SG;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_ZA;

@ComaasPlugin
@Profile({TENANT_MX, TENANT_AR, TENANT_ZA, TENANT_SG})
@Component
public class CommonAttributeFilterFactory implements FilterFactory {
	public static final String IDENTIFIER = "com.ecg.replyts.commonattributefilter.CommonAttributeFilterFactory";

    @Override
	public Filter createPlugin(String filterName, JsonNode jsonNode) {
        return new CommonAttributeFilter(PatternRulesParser.parse(jsonNode));
    }

	@Override
	public String getIdentifier() {
	    return IDENTIFIER;
	}
}
