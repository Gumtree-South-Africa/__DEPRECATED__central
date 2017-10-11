package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.IpAddressExtractor;
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

class Ip2CountryFilter implements Filter {

    private final static Logger LOG = LoggerFactory.getLogger(Ip2CountryFilter.class);

    private final Ip2CountryRules countryRules;
    private final Ip2CountryResolver resolver;
    private final IpAddressExtractor extractor;


    public Ip2CountryFilter(Ip2CountryRules countryRules, Ip2CountryResolver resolver, IpAddressExtractor extractor) {
        this.countryRules = countryRules;
        this.resolver = resolver;
        this.extractor = extractor;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Optional<String> ipStr = extractor.retrieveIpAddress(messageProcessingContext);
        if (!ipStr.isPresent()) {
            LOG.trace("Message does not have an IP address header");
            return emptyList();
        }


        LOG.trace("Finding country for IP Address {}", ipStr.get());
        Optional<String> resolveCountryCode = resolver.resolve(ipStr.get());
        LOG.trace("IP {} is from country {}", ipStr.get(), resolveCountryCode.orElse("UNRESOLVABLE"));
        if (!resolveCountryCode.isPresent())
            return emptyList();

        int scoreForCountry = countryRules.getScoreForCountry(resolveCountryCode.get());
        if (scoreForCountry == 0)
            return emptyList();


        return ImmutableList.of(
                new FilterFeedback(
                        resolveCountryCode.get(),
                        "Mail from country: " + resolveCountryCode.get(),
                        scoreForCountry,
                        FilterResultState.OK
                ));
    }
}
