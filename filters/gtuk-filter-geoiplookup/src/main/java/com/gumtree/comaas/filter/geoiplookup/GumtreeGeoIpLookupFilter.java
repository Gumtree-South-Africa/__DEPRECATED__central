package com.gumtree.comaas.filter.geoiplookup;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.common.geoip.GeoIpService;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.GeoIpLookupConfig;
import com.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.longDescription;
import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.resultFilterResultMap;

@Component
public class GumtreeGeoIpLookupFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeGeoIpLookupFilter.class);

    private static final Timer TIMER = TimingReports.newTimer("geoiplookup-filter-process-time");

    private Filter pluginConfig;
    private GeoIpLookupConfig filterConfig;

    private GeoIpService geoIpService;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) throws ProcessingTimeExceededException {
        try (Timer.Context ignore = TIMER.time()) {
            if (!messageContext.getMessage().getMessageDirection().equals(MessageDirection.BUYER_TO_SELLER)) {
                return Collections.emptyList();
            }

            String ip = messageContext.getMessage().getHeaders().get(GumtreeCustomHeaders.BUYER_IP.getHeaderValue());
            if (ip == null) {
                return Collections.emptyList();
            }

            LOG.debug("About to check IP {}", ip);

            Optional<String> countryOpt = lookupCountry(ip);
            if (!countryOpt.isPresent()) {
                return Collections.emptyList();
            }

            String country = countryOpt.get();
            LOG.debug("IP {} - origin is {}", ip, country);

            // Output an 'OK' reason with the found country code
            messageContext.getFilterContext().put("country", country);

            String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), country);

            return Collections.singletonList(new FilterFeedback("Lookup country " + country, description, 0, resultFilterResultMap.get(filterConfig.getResult())));
        }
    }

    private Optional<String> lookupCountry(String ip) {
        try {
            long start = System.currentTimeMillis();
            String country = geoIpService.getCountryCode(ip);
            long end = System.currentTimeMillis();
            LOG.debug("IP lookup query took " + (end - start) + "ms");

            if (StringUtils.isBlank(country)) {
                LOG.debug("Could not determine location type of Ip {}", ip);
                return Optional.empty();
            }

            LOG.debug("Location for IP {} is {}", ip, country);
            return Optional.of(country);

        } catch (Exception e) {
            LOG.error("Couldn't score ip", e);
            return Optional.empty();
        }
    }

    GumtreeGeoIpLookupFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeGeoIpLookupFilter withFilterConfig(GeoIpLookupConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }

    GumtreeGeoIpLookupFilter withGeoIPService(GeoIpService geoIPService) {
        this.geoIpService = geoIPService;
        return this;
    }
}