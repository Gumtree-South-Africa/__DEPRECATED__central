package com.ecg.comaas.it.filter.quickreply;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ComaasPlugin
@Component
public class QuickReplyFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ebay.columbus.replyts2.quickreply.QuickReplyFilterFactory";

    private static final Logger LOG = LoggerFactory.getLogger(QuickReplyFilterFactory.class);

    private List<HeaderEntry> resolvedCustomHeaders;

    @Autowired
    public QuickReplyFilterFactory(@Value("${replyts.quickreply.customHeaders}") String customHeaders) {
        Preconditions.checkNotNull(customHeaders, "customHeaders cannot be null, at least it must be an empty list");

        this.resolvedCustomHeaders = Arrays.stream(customHeaders.split(","))
          .filter(StringUtils::hasText)
          .map(String::trim)
          .map(this::toHeader)
          .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        Preconditions.checkNotNull(this.resolvedCustomHeaders, "resolvedCustomHeaders cannot be null");

        LOG.trace("QuickReply resolved custom headers {}", resolvedCustomHeaders);
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new QuickReplyFilter(this.resolvedCustomHeaders);
    }


    private HeaderEntry toHeader(String header) {
        String[] splitHeader = header.split("\\|");

        if (splitHeader.length != 2) {
            LOG.error("QuickReply plugin configuration error: header not properly configured");

            throw new IllegalArgumentException("QuickReply plugin configuration error: header not properly configured");
        }

        return new HeaderEntry(splitHeader[0], Integer.parseInt(splitHeader[1]));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
