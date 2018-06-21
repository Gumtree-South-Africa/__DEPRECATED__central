package com.ecg.comaas.kjca.filter.countryblocked;

import com.ecg.comaas.kjca.coremod.shared.BoxHeaders;
import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ecg.comaas.kjca.coremod.shared.BoxHeaders.SENDER_IP_ADDRESS;
import static com.ecg.comaas.kjca.filter.countryblocked.CountryBlockedFilter.IS_COUNTRY_BLOCKED_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountryBlockedFilterTest {

    private static final int SCORE = 100;
    private static final String IP_ADDRESS = "1.2.3.4";

    private CountryBlockedFilter objectUnderTest;

    @Mock
    private TnsApiClient tnsApiClientMock;

    @Mock
    private MessageProcessingContext messageContextMock;

    @Mock
    private Mail mailMock;

    @Before
    public void setUp() {
        objectUnderTest = new CountryBlockedFilter(SCORE, tnsApiClientMock);
        when(messageContextMock.getMail()).thenReturn(Optional.of(mailMock));
    }

    @Test
    public void whenIpIsNull_shouldReturnEmptyFeedback() {
        when(mailMock.getUniqueHeaders()).thenReturn(ImmutableMap.of(SENDER_IP_ADDRESS.getHeaderName(), IP_ADDRESS));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenIpIsEmpty_shouldReturnEmptyFeedback() {
        when(mailMock.getUniqueHeaders()).thenReturn(ImmutableMap.of(SENDER_IP_ADDRESS.getHeaderName(), IP_ADDRESS));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenNoResultFromTnsApi_shouldReturnEmptyFeedback() {
        when(mailMock.getUniqueHeaders()).thenReturn(ImmutableMap.of(SENDER_IP_ADDRESS.getHeaderName(), IP_ADDRESS));
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(Collections.emptyMap());

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenTnsApiReturnsFalse_shouldReturnEmptyFeedback() {
        when(mailMock.getUniqueHeaders()).thenReturn(ImmutableMap.of(SENDER_IP_ADDRESS.getHeaderName(), IP_ADDRESS));
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(Collections.singletonMap(IS_COUNTRY_BLOCKED_KEY, Boolean.FALSE));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenTnsApiReturnsTrue_shouldReturnFeedback() {
        when(mailMock.getUniqueHeaders()).thenReturn(ImmutableMap.of(SENDER_IP_ADDRESS.getHeaderName(), IP_ADDRESS));
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(Collections.singletonMap(IS_COUNTRY_BLOCKED_KEY, Boolean.TRUE));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).containsExactly(
                new FilterFeedback("country is blocked", "IP country is blocked", SCORE, FilterResultState.DROPPED)
        );
    }
}
