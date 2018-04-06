package com.ecg.replyts.integration.test.filter;


import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

public class ExampleFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.replyts.integration.test.filter.ExampleFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new Filter() {
            @Override
            public List<FilterFeedback> filter(MessageProcessingContext context) {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
