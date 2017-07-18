package com.ecg.kijijiit.conversationmonitor;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by fmaffioletti on 28/07/14.
 */
@ComaasPlugin
@Configuration
@ComponentScan("com.ebay.columbus.replyts2.conversationmonitor")
public class ConversationMonitorFilterConfiguration {
    private static final Logger LOG =
                    LoggerFactory.getLogger(ConversationMonitorFilterConfiguration.class);

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
                .filter(tc -> StringUtils.hasText(tc))
                .map(tc ->(char) Integer.parseInt(tc.trim()))
                .map(tc -> Character.toString(tc))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    @Bean
    public FilterFactory filterFactory() {
        return new ConversationMonitorFilterFactory(warnSizeThreshold, errorSizeThreshold, triggerCharsList, thresholdCheckEnabled);
    }

}
