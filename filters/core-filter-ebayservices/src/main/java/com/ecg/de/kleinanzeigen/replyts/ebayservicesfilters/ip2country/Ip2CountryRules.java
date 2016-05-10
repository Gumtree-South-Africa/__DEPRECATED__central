package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Map;

/**
 * User: acharton
 * Date: 12/17/12
 */
class Ip2CountryRules {

    private final int defaultScore;
    private final Map<String, Integer> countryScores;

    public Ip2CountryRules(int defaultScore, Map<String, Integer> countryScores) {
        this.defaultScore = defaultScore;
        this.countryScores = countryScores;
    }

    public int getScoreForCountry(String country) {
        Preconditions.checkNotNull(country);
        Optional<Integer> scoreFromCountry = Optional.fromNullable(countryScores.get(country.toLowerCase()));
        return scoreFromCountry.or(defaultScore);
    }
}
