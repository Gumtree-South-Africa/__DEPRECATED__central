package com.ecg.de.mobile.replyts.badword;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
class BadwordFilterFactory implements FilterFactory {

    private final CsFilterServiceClient csFilterServiceClient;

    public BadwordFilterFactory(CsFilterServiceClient csFilterServiceClient) {
        this.csFilterServiceClient = csFilterServiceClient;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BadwordFilter(csFilterServiceClient);
    }
}
