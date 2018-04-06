package com.ecg.replyts.integration.test.filter;


import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class RejectingConfigurationFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.replyts.integration.test.filter.RejectingConfigurationFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        throw new IllegalStateException("configuration rejected");
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
