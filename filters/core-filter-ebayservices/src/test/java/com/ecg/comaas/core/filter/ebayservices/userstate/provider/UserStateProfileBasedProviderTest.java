package com.ecg.comaas.core.filter.ebayservices.userstate.provider;

import com.ebay.marketplace.user.v1.services.UserEnum;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserProfileService;
import de.mobile.ebay.service.userprofile.domain.AccountStatus;
import de.mobile.ebay.service.userprofile.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserStateProfileBasedProviderTest {

    private static final String DEFAULT_SENDER = "buyer@example.com";

    private UserStateProvider userStateProvider;

    @Mock
    private UserProfileService userProfileServiceMock;

    @Mock
    private User userMock;

    @Before
    public void setUp() throws ServiceException {
        when(userProfileServiceMock.getUser(DEFAULT_SENDER)).thenReturn(userMock);
        when(userMock.getUserAccountStatus()).thenReturn(AccountStatus.CONFIRMED);
        userStateProvider = new UserStateProfileBasedProvider(userProfileServiceMock);
    }

    @Test
    public void whenProfileServiceThrowsException_shouldReturnNull() throws ServiceException {
        when(userProfileServiceMock.getUser(anyString())).thenThrow(new ServiceException("exception"));
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isNull();
    }

    @Test
    public void whenUserNull_shouldReturnNull() throws ServiceException {
        when(userProfileServiceMock.getUser(anyString())).thenReturn(null);
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isNull();
    }

    @Test
    public void whenAccountStatusNull_shouldReturnNull() throws ServiceException {
        when(userMock.getUserAccountStatus()).thenReturn(null);
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isNull();
    }

    @Test
    public void whenAccountStatusConfirmed_shouldReturnConfirmed() throws ServiceException {
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isEqualTo(UserEnum.CONFIRMED);
    }

    @Test
    public void whenAccountStatusSuspended_shouldReturnSuspended() throws ServiceException {
        when(userMock.getUserAccountStatus()).thenReturn(AccountStatus.SUSPENDED);
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isEqualTo(UserEnum.SUSPENDED);
    }

    @Test
    public void whenAccountStatusDeleted_shouldReturnUnknown() throws ServiceException {
        when(userMock.getUserAccountStatus()).thenReturn(AccountStatus.DELETED);
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isEqualTo(UserEnum.UNKNOWN);
    }
}
