package com.ecg.comaas.core.filter.ebayservices.userstate;

import com.ebay.marketplace.user.v1.services.UserEnum;
import com.ecg.comaas.core.filter.ebayservices.userstate.provider.UserStateProvider;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import de.mobile.ebay.service.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserStateFilterTest {

    private UserStateFilter userStateFilter;

    @Mock
    private UserStateFilterConfigHolder configHolderMock;

    @Mock
    private UserStateProvider userStateProviderMock;

    @Mock
    private MessageProcessingContext contextMock;

    @Before
    public void setUp() throws ServiceException {
        Mail mailMock = mock(Mail.class);
        when(mailMock.getFrom()).thenReturn("buyer@example.com");
        when(contextMock.getMail()).thenReturn(Optional.of(mailMock));
        when(userStateProviderMock.getSenderState("buyer@example.com")).thenReturn(UserEnum.CONFIRMED);
        when(configHolderMock.getUserStateScore(UserEnum.CONFIRMED)).thenReturn(50);
        userStateFilter = new UserStateFilter(configHolderMock, userStateProviderMock);
    }

    @Test
    public void whenNoMail_shouldReturnEmptyFeedback() {
        when(contextMock.getMail()).thenReturn(Optional.empty());
        List<FilterFeedback> actual = userStateFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenUserStateNull_shouldReturnEmptyFeedback() {
        when(userStateProviderMock.getSenderState(anyString())).thenReturn(null);
        List<FilterFeedback> actual = userStateFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenUserStateScoreIsZero_shouldReturnEmptyFeedback() throws ServiceException {
        when(configHolderMock.getUserStateScore(UserEnum.CONFIRMED)).thenReturn(0);
        List<FilterFeedback> actual = userStateFilter.filter(contextMock);
        assertThat(actual).isEmpty();
    }

    @Test
    public void whenUserStateScoreIsNotZero_shouldReturnProperFeedback() throws ServiceException {
        List<FilterFeedback> actual = userStateFilter.filter(contextMock);
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingFieldByField(new FilterFeedback("buyer@example.com",
                "User state is: CONFIRMED", 50, FilterResultState.OK));
    }
}
