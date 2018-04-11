package com.ecg.comaas.gtau.filter.ebayservices.ip2country;

import com.ecg.comaas.gtau.filter.ebayservices.IpAddressExtractor;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class Ip2CountryFilterFactory implements FilterFactory {

    private static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country.Ip2CountryFilterFactory";

    private final Ip2CountryResolver ip2CountryResolver;

    public Ip2CountryFilterFactory(Ip2CountryResolver ip2CountryResolver) {
        this.ip2CountryResolver = ip2CountryResolver;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new Ip2CountryFilter(parse(jsonNode), ip2CountryResolver, new IpAddressExtractor());
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
