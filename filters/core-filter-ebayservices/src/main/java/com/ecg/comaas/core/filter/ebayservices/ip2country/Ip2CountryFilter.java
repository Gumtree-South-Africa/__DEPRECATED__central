package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.comaas.core.filter.ebayservices.IpAddressExtractor;
import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.LocationProvider;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class Ip2CountryFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(Ip2CountryFilter.class);

    private final Ip2CountryFilterConfigHolder ip2CountryFilterConfigHolder;
    private final LocationProvider locationProvider;

    public Ip2CountryFilter(Ip2CountryFilterConfigHolder ip2CountryFilterConfigHolder, LocationProvider locationProvider) {
        this.ip2CountryFilterConfigHolder = ip2CountryFilterConfigHolder;
        this.locationProvider = locationProvider;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Optional<String> ipAddress = IpAddressExtractor.retrieveIpAddress(messageProcessingContext);
        if (!ipAddress.isPresent()) {
            LOG.trace("Message does not have either IP address header or provided IP address is not valid");
            return emptyList();
        }

        LOG.trace("Finding country for IP Address {}", ipAddress.get());
        Optional<String> resolveCountryCode = locationProvider.getCountry(ipAddress.get());
        LOG.trace("IP {} is from country {}", ipAddress.get(), resolveCountryCode.orElse("UNRESOLVABLE"));
        if (!resolveCountryCode.isPresent()) {
            return emptyList();
        }

        int scoreForCountry = ip2CountryFilterConfigHolder.getCountryScore(resolveCountryCode.get());
        if (scoreForCountry == 0) {
            return emptyList();
        }

        return ImmutableList.of(new FilterFeedback(resolveCountryCode.get(), "Mail from country: " + resolveCountryCode.get(),
                scoreForCountry, FilterResultState.OK));
    }
}
