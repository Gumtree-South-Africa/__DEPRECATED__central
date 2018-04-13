package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.comaas.core.filter.ebayservices.ip2country.Ip2CountryRules;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;

import static junit.framework.Assert.assertEquals;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class Ip2CountryRulesTest {

    @Test
    public void useDefaultScoreIfCountryNotFound() throws Exception {
        assertEquals(77, new Ip2CountryRules(77, Collections.<String, Integer>emptyMap()).getScoreForCountry("unknown states"));
    }

    @Test
    public void matchCountry() throws Exception {
        assertEquals(-100, new Ip2CountryRules(100, ImmutableMap.of("at", -100, "de", 100)).getScoreForCountry("AT"));
    }

}
