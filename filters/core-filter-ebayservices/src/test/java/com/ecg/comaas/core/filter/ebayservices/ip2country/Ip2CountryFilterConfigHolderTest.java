package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Ip2CountryFilterConfigHolderTest {

    private Ip2CountryFilterConfigHolder ip2CountryFilterConfigHolder;

    @Test(expected = IllegalArgumentException.class)
    public void whenEmptyConfig_shouldThrowException() {
        ip2CountryFilterConfigHolder = new Ip2CountryFilterConfigHolder(JsonObjects.builder().build());
        ip2CountryFilterConfigHolder.getCountryScore("NL");
    }

    @Test(expected = NullPointerException.class)
    public void whenNoCountryProvided_shouldThrowException() {
        ip2CountryFilterConfigHolder = new Ip2CountryFilterConfigHolder(JsonObjects.builder().attr("NL", "50").build());
        ip2CountryFilterConfigHolder.getCountryScore(null);
    }

    @Test
    public void whenCountryNotFound_shouldReturnZero() {
        ip2CountryFilterConfigHolder = new Ip2CountryFilterConfigHolder(JsonObjects.builder().attr("NL", "50").build());
        int actual = ip2CountryFilterConfigHolder.getCountryScore("BE");
        assertThat(actual).isZero();
    }

    @Test
    public void whenCountryFound_shouldReturnCountryScore() {
        ip2CountryFilterConfigHolder = new Ip2CountryFilterConfigHolder(JsonObjects.builder().attr("NL", "50").build());
        int actual = ip2CountryFilterConfigHolder.getCountryScore("NL");
        assertThat(actual).isEqualTo(50);
    }
}
