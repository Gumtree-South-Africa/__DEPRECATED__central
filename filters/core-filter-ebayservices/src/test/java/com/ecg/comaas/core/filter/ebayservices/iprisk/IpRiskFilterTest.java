package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ebay.marketplace.security.v1.services.IPBadLevel;
import com.ebay.marketplace.security.v1.services.IPRatingInfo;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IpRiskFilterTest {

    private IpRiskFilter ipRiskFilter;

    @Mock
    private IpRiskFilterConfigHolder configHolderMock;

    @Mock
    private IpRatingService ratingServiceMock;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext contextMock;

    @Mock
    private IPRatingInfo ratingInfoMock;

    @Before
    public void setUp() throws Exception {
        setIpAddress("127.0.0.1");
        when(ratingServiceMock.getIpRating("127.0.0.1")).thenReturn(ratingInfoMock);
        when(ratingInfoMock.getIpBadLevel()).thenReturn(IPBadLevel.BAD);
        when(configHolderMock.getRating(IPBadLevel.BAD)).thenReturn(50);
        ipRiskFilter = new IpRiskFilter(configHolderMock, ratingServiceMock);
    }

    private void setIpAddress(String ipAddress) {
        Mail mailMock = mock(Mail.class);
        when(contextMock.getMail()).thenReturn(Optional.of(mailMock));
        when(contextMock.getConversation().getMessages().size()).thenReturn(1);
        when(mailMock.getUniqueHeader("X-Cust-Ip")).thenReturn(ipAddress);
    }

    @Test
    public void whenNoIpAddress_shouldReturnEmptyFeedback() {
        setIpAddress(null);
        List<FilterFeedback> actual = ipRiskFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenIpRatingServiceThrowsException_shouldReturnEmptyFeedback() throws ServiceException {
        when(ratingServiceMock.getIpRating(anyString())).thenThrow(new ServiceException("exception"));
        List<FilterFeedback> actual = ipRiskFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenIpRatingNull_shouldReturnEmptyFeedback() throws ServiceException {
        when(ratingServiceMock.getIpRating(anyString())).thenReturn(null);
        List<FilterFeedback> actual = ipRiskFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenIpBadLevelNull_shouldReturnEmptyFeedback() throws ServiceException {
        when(ratingInfoMock.getIpBadLevel()).thenReturn(null);
        List<FilterFeedback> actual = ipRiskFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenScoreZero_shouldReturnEmptyFeedback() throws ServiceException {
        when(configHolderMock.getRating(any(IPBadLevel.class))).thenReturn(0);
        List<FilterFeedback> actual = ipRiskFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenScoreNotZero_shouldReturnProperFeedback() throws ServiceException {
        List<FilterFeedback> actual = ipRiskFilter.filter(contextMock);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingFieldByField(new FilterFeedback("127.0.0.1",
                "IP is rated as: BAD", 50, FilterResultState.OK));
    }
}
