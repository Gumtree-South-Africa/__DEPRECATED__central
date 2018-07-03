package com.ecg.comaas.core.filter.ebayservices.userstate;

import com.ebay.marketplace.user.v1.services.UserEnum;
import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserStateFilterConfigHolderTest {

    private UserStateFilterConfigHolder configHolder;

    @Test(expected = IllegalArgumentException.class)
    public void whenEmptyConfig_shouldThrowException() {
        configHolder = new UserStateFilterConfigHolder(JsonObjects.builder().build());
    }

    @Test
    public void whenUserStateNotFound_shouldReturnZero() {
        configHolder = new UserStateFilterConfigHolder(JsonObjects.builder().attr(UserEnum.SUSPENDED.name(), "50").build());
        int actual = configHolder.getUserStateScore(UserEnum.UNKNOWN);
        assertThat(actual).isZero();
    }

    @Test
    public void whenRatingFound_shouldReturnRating() {
        configHolder = new UserStateFilterConfigHolder(JsonObjects.builder().attr(UserEnum.SUSPENDED.name(), "50").build());
        int actual = configHolder.getUserStateScore(UserEnum.SUSPENDED);
        assertThat(actual).isEqualTo(50);
    }
}
