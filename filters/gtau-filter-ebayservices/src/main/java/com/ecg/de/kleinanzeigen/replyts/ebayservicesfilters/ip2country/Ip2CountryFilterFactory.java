package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.IpAddressExtractor;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * User: acharton
 * Date: 12/17/12
 */
@Component
class Ip2CountryFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country.Ip2CountryFilterFactory";

    private final Ip2CountryResolver ip2CountryResolver;

    @Autowired
    public Ip2CountryFilterFactory(Ip2CountryResolver ip2CountryResolver) {
        this.ip2CountryResolver = ip2CountryResolver;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new Ip2CountryFilter(s, parse(jsonNode), ip2CountryResolver, new IpAddressExtractor());
    }

    private Ip2CountryRules parse(JsonNode jsonNode) {
        CountryRulesParser crp = new CountryRulesParser(jsonNode);
        int defaultScore = 50; // read from json
        Map<String, Integer> countryScores = null; // read from json
        return new Ip2CountryRules(crp.getDefaultScore(), crp.getCountryScores());
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
