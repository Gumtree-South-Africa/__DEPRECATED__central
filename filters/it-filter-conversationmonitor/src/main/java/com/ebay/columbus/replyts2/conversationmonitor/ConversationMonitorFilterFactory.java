package com.ebay.columbus.replyts2.conversationmonitor;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConversationMonitorFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ebay.columbus.replyts2.conversationmonitor.ConversationMonitorFilterFactory";

    private static final Logger LOG = LoggerFactory.getLogger(ConversationMonitorFilterFactory.class);

    @Value("${replyts.conversation.monitor.trigger.chars}")
    private String triggerChars;

    @Value("${replyts.conversation.monitor.threshold.check.enabled}")
    private boolean thresholdCheckEnabled;

    @Value("${replyts.conversation.monitor.warn.size.threshold}")
    private Long warnSizeThreshold;

    @Value("${replyts.conversation.monitor.error.size.threshold}")
    private Long errorSizeThreshold;

    private List<String> triggerCharsList;

    @PostConstruct
    public void setupTriggerCharsList() {
        LOG.debug("Conversation monitor warn size threshold " + this.warnSizeThreshold);
        LOG.debug("Conversation monitor error size threshold " + this.errorSizeThreshold);
        LOG.debug("Conversation monitor threshold check enabled " + this.thresholdCheckEnabled);

        triggerCharsList = Arrays.stream(triggerChars.split(","))
          .filter(StringUtils::hasText)
          .map(tc ->(char) Integer.parseInt(tc.trim()))
          .map(tc -> Character.toString(tc))
          .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new ConversationMonitorFilter(warnSizeThreshold, errorSizeThreshold, triggerCharsList, thresholdCheckEnabled);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}