package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.LocationProvider;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Ip2CountryFilterTest {

    private Ip2CountryFilter ip2CountryFilter;

    @Mock
    private Ip2CountryFilterConfigHolder configHolderMock;

    @Mock
    private LocationProvider locationProviderMock;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext contextMock;

    @Before
    public void setUp() throws Exception {
        setIpAddress("10.0.0.1");
        when(locationProviderMock.getCountry("10.0.0.1")).thenReturn(Optional.of("NL"));
        when(configHolderMock.getCountryScore(anyString())).thenReturn(50);
        ip2CountryFilter = new Ip2CountryFilter(configHolderMock, locationProviderMock);
    }

    private void setIpAddress(String ipAddress) {
        Mail mailMock = mock(Mail.class);
        when(contextMock.getMail()).thenReturn(Optional.of(mailMock));
        when(contextMock.getConversation().getMessages().size()).thenReturn(1);
        when(mailMock.getUniqueHeader("X-Cust-Ip")).thenReturn(ipAddress);
    }

    @Test
    public void whenIpAddressNotPresent_shouldReturnEmptyFeedback() {
        setIpAddress(null);
        List<FilterFeedback> actual = ip2CountryFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenCountryNotPresent_shouldReturnEmptyFeedback() {
        when(locationProviderMock.getCountry(anyString())).thenReturn(Optional.empty());
        List<FilterFeedback> actual = ip2CountryFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenCountryScoreZero_shouldReturnEmptyFeedback() {
        when(configHolderMock.getCountryScore(anyString())).thenReturn(0);
        List<FilterFeedback> actual = ip2CountryFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenCountryScoreNotZero_shouldReturnProperFeedback() {
        List<FilterFeedback> actual = ip2CountryFilter.filter(contextMock);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingFieldByField(
                new FilterFeedback("NL", "Mail from country: NL", 50, FilterResultState.OK));
    }
}
