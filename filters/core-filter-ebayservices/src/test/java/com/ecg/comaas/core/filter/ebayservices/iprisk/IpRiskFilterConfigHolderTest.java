package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ebay.marketplace.security.v1.services.IPBadLevel;
import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IpRiskFilterConfigHolderTest {

    private IpRiskFilterConfigHolder configHolder;

    @Test(expected = IllegalArgumentException.class)
    public void whenEmptyConfig_shouldThrowException() {
        configHolder = new IpRiskFilterConfigHolder(JsonObjects.builder().build());
    }

    @Test
    public void whenRatingNotFound_shouldReturnZero() {
        configHolder = new IpRiskFilterConfigHolder(JsonObjects.builder().attr(IPBadLevel.BAD.name(), "100").build());
        int actual = configHolder.getRating(IPBadLevel.VERY_BAD);
        assertThat(actual).isZero();
    }

    @Test
    public void whenRatingFound_shouldReturnRating() {
        configHolder = new IpRiskFilterConfigHolder(JsonObjects.builder().attr(IPBadLevel.BAD.name(), "100").build());
        int actual = configHolder.getRating(IPBadLevel.BAD);
        assertThat(actual).isEqualTo(100);
    }
}
