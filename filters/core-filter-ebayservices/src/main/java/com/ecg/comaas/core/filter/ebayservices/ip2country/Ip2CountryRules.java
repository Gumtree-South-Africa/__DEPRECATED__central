package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.Optional;

class Ip2CountryRules {

    private final int defaultScore;
    private final Map<String, Integer> countryScores;

    public Ip2CountryRules(int defaultScore, Map<String, Integer> countryScores) {
        this.defaultScore = defaultScore;
        this.countryScores = countryScores;
    }

    public int getScoreForCountry(String country) {
        Preconditions.checkNotNull(country);
        Optional<Integer> scoreFromCountry = Optional.ofNullable(countryScores.get(country.toLowerCase()));
        return scoreFromCountry.orElse(defaultScore);
    }
}
