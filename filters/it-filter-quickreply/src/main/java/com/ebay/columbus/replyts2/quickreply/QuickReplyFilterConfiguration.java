package com.ebay.columbus.replyts2.quickreply;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by fmaffioletti on 28/07/14.
 */
@ComaasPlugin
@Configuration
@ComponentScan("com.ebay.columbus.replyts2.quickreply")
public class QuickReplyFilterConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(QuickReplyFilterConfiguration.class);

    private List<HeaderEntry> resolvedCustomHeaders;

    public QuickReplyFilterConfiguration(@Value("${replyts.quickreply.customHeaders}") String customHeaders) {
        Preconditions.checkNotNull(customHeaders, "customHeaders cannot be null, at least it must be an empty list");
        this.resolvedCustomHeaders = Arrays.stream(customHeaders.split(","))
                .filter(s -> StringUtils.hasText(s))
                .map(s -> s.trim())
                .map(this::toHeader)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        Preconditions.checkNotNull(this.resolvedCustomHeaders, "resolvedCustomHeaders cannot be null");
        LOG.debug("QuickReply resolved custom headers " + resolvedCustomHeaders);
    }

    @Bean
    public FilterFactory filterFactory() {
        return new QuickReplyFilterFactory(resolvedCustomHeaders);
    }

    private HeaderEntry toHeader(String header) {
        String[] splitHeader = header.split("\\|");
        if (splitHeader.length != 2) {
            LOG.error("QuickReply plugin configuration error: header not properly configured");
            throw new IllegalArgumentException("QuickReply plugin configuration error: header not properly configured");
        }
        return new HeaderEntry(splitHeader[0], Integer.parseInt(splitHeader[1]));
    }

}
