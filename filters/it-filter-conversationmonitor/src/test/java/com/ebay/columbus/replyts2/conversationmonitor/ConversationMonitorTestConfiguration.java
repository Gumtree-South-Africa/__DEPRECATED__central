package com.ebay.columbus.replyts2.conversationmonitor;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jaludden on 02/05/17.
 */
@Configuration
public class ConversationMonitorTestConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        List<String> chars = Arrays.stream("65533,189".split(","))
                .filter(tc -> StringUtils.hasText(tc))
                .map(tc -> (char) Integer.parseInt(tc.trim()))
                .map(tc -> Character.toString(tc))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        return new ConversationMonitorFilterFactory(5L, 10L, chars, true);
    }


}
