package com.ebay.columbus.replyts2.quickreply;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Created by jaludden on 05/05/17.
 */
public class QuickReplyFilterFactory implements FilterFactory {

    private List<HeaderEntry> resolvedCustomHeaders;

    public QuickReplyFilterFactory(List<HeaderEntry> resolvedCustomHeaders) {
        this.resolvedCustomHeaders = resolvedCustomHeaders;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new QuickReplyFilter(this.resolvedCustomHeaders);
    }
}
