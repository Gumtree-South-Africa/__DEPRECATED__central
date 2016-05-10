package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Map;

/**
 * Read the config from ad json node, transform all country codes to lower case.
 *
 * User: acharton
 * Date: 12/18/12
 */
class CountryRulesParser {

    public static final String DEFAULT_KEY = "DEFAULT";

    private Map<String, Integer> countryScores;
    private int defaultScore;

    public CountryRulesParser(JsonNode jsonNode) {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.<String, Integer>builder();
        Iterator<Map.Entry<String,JsonNode>> rules = jsonNode.fields();
        Preconditions.checkArgument(rules != null && rules.hasNext(), "given config does not contain a rules element.");

        while(rules.hasNext()) {
            Map.Entry<String,JsonNode> rule = rules.next();
            if(defaultValue(rule.getKey())){
               defaultScore = rule.getValue().asInt();
            } else {
                builder.put(rule.getKey().toLowerCase(), rule.getValue().asInt());
            }
        }

        countryScores = builder.build();
    }

    private boolean defaultValue(String key) {
        return key.equals(DEFAULT_KEY);
    }

    public Map<String,Integer> getCountryScores() {
        return countryScores;
    }

    public int getDefaultScore() {
        return defaultScore;
    }
}
