package com.ecg.replyts.commonattributefilter;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@ComaasPlugin
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
