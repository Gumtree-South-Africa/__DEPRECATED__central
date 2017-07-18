package com.ecg.kijijiit.conversationmonitor;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Created by jaludden on 08/05/17.
 */
public class ConversationMonitorFilterFactory implements FilterFactory {
    private final Long warnSizeThreshold;
    private final Long errorSizeThreshold;
    private final List<String> triggerCharsList;
    private final boolean thresholdCheckEnabled;

    public ConversationMonitorFilterFactory(Long warnSizeThreshold, Long errorSizeThreshold, List<String> triggerCharsList, boolean thresholdCheckEnabled) {
        this.warnSizeThreshold = warnSizeThreshold;
        this.errorSizeThreshold = errorSizeThreshold;
        this.triggerCharsList = triggerCharsList;
        this.thresholdCheckEnabled = thresholdCheckEnabled;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new ConversationMonitorFilter(warnSizeThreshold, errorSizeThreshold, triggerCharsList, thresholdCheckEnabled);
    }
}
