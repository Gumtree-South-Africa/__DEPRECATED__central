package com.ecg.comaas.core.filter.ebayservices.userstate.provider;

import com.ebay.marketplace.user.v1.services.MemberBadgeDataType;
import com.ebay.marketplace.user.v1.services.UserEnum;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserStateBadgeBasedProviderTest {

    private static final String DEFAULT_SENDER = "buyer@example.com";

    private UserStateProvider userStateProvider;

    @Mock
    private UserService userServiceMock;

    @Mock
    private MemberBadgeDataType badgeMock;

    @Before
    public void setUp() throws ServiceException {
        when(userServiceMock.getMemberBadgeData(DEFAULT_SENDER)).thenReturn(badgeMock);
        when(badgeMock.getUserState()).thenReturn(UserEnum.CONFIRMED);
        userStateProvider = new UserStateBadgeBasedProvider(userServiceMock);
    }

    @Test
    public void whenUserServiceThrowsException_shouldReturnNull() throws ServiceException {
        when(userServiceMock.getMemberBadgeData(anyString())).thenThrow(new ServiceException("exception"));
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isNull();
    }

    @Test
    public void whenBadgeNull_shouldReturnNull() throws ServiceException {
        when(userServiceMock.getMemberBadgeData(anyString())).thenReturn(null);
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isNull();
    }

    @Test
    public void whenUserStateNull_shouldReturnNull() throws ServiceException {
        when(badgeMock.getUserState()).thenReturn(null);
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isNull();
    }

    @Test
    public void whenUserStateNotNull_shouldReturnUserState() throws ServiceException {
        UserEnum actual = userStateProvider.getSenderState(DEFAULT_SENDER);
        assertThat(actual).isEqualTo(UserEnum.CONFIRMED);
    }
}
