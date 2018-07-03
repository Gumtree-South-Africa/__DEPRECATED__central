package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Ip2CountryFilterConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(Ip2CountryFilterConfigHolder.class);
    private static final String DEFAULT_KEY = "DEFAULT";

    private final Map<String, Integer> countryScores = new HashMap<>();

    private int defaultScore = 0;

    public Ip2CountryFilterConfigHolder(JsonNode jsonNode) {
        Iterator<Map.Entry<String, JsonNode>> rulesIterator = jsonNode.fields();
        if (rulesIterator == null || !rulesIterator.hasNext()) {
            String errorMessage = "Config does not contain rules: " + jsonNode.toString();
            LOG.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        while (rulesIterator.hasNext()) {
            Map.Entry<String, JsonNode> rule = rulesIterator.next();
            if (DEFAULT_KEY.equalsIgnoreCase(rule.getKey())) {
                this.defaultScore = rule.getValue().asInt();
            } else {
                this.countryScores.put(rule.getKey().toLowerCase(), rule.getValue().asInt());
            }
        }
    }

    public int getCountryScore(String country) {
        if (Strings.isNullOrEmpty(country)) {
            String errorMessage = "Country name is incorrect: " + country;
            LOG.error(errorMessage);
            throw new NullPointerException(errorMessage);
        }
        Integer countryScore = countryScores.get(country.toLowerCase());
        return countryScore == null ? defaultScore : countryScore;
    }
}
