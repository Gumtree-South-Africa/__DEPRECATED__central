package com.ecg.mde.filter.badword;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

class BadwordFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ecg.de.mobile.replyts.badword.BadwordFilterFactory";

    private final CsFilterServiceClient csFilterServiceClient;

    public BadwordFilterFactory(CsFilterServiceClient csFilterServiceClient) {
        this.csFilterServiceClient = csFilterServiceClient;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BadwordFilter(csFilterServiceClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
