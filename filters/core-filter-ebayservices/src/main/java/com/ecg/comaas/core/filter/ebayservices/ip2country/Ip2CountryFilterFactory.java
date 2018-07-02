package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.LocationProvider;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class Ip2CountryFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country.Ip2CountryFilterFactory";

    private final LocationProvider locationProvider;

    public Ip2CountryFilterFactory(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new Ip2CountryFilter(new Ip2CountryFilterConfigHolder(jsonNode), locationProvider);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
