package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserProfileService;
import de.mobile.ebay.service.userprofile.domain.AccountStatus;
import de.mobile.ebay.service.userprofile.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * User: acharton
 * Date: 12/18/12
 */
@RunWith(MockitoJUnitRunner.class)
public class UserStateFilterTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private User userFromService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext mpc;
    @Mock
    private Mail mail;
    private UserStateFilter userStateFilter;

    @Before
    public void setUp() throws Exception {
        when(userProfileService.getUser(anyString())).thenReturn(userFromService);
        when(mpc.getMail()).thenReturn(Optional.of(mail));
        when(mail.getFrom()).thenReturn("sender@test.de");
        userStateFilter = new UserStateFilter(ImmutableMap.of("UNKNOWN", 0, "CONFIRMED", -50, "SUSPENDED", 100), userProfileService);
    }

    @Test
    public void rateBadUser() throws Exception {
        when(userFromService.getUserAccountStatus()).thenReturn(AccountStatus.SUSPENDED);

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertEquals(1, feedbacks.size());
        FilterFeedback filterFeedback = feedbacks.get(0);
        assertThat(filterFeedback.getDescription()).isEqualTo("User state is: SUSPENDED");
        assertThat(filterFeedback.getResultState()).isEqualTo(FilterResultState.OK);
        assertThat(filterFeedback.getScore()).isEqualTo(100);
        assertThat(filterFeedback.getUiHint()).isEqualTo("sender@test.de");
    }

    @Test
    public void ignoreUnknown() throws Exception {
        when(userFromService.getUserAccountStatus()).thenReturn(AccountStatus.UNCONFIRMED);

        UserStateFilter userStateFilter = new UserStateFilter(ImmutableMap.of("UNKNOWN", 0, "CONFIRMED", -50, "SUSPENDED", 100), userProfileService);

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }

    @Test
    public void ignoreEmptyUserState() throws Exception {
        when(userProfileService.getUser(anyString())).thenReturn(null);

        UserStateFilter userStateFilter = new UserStateFilter(ImmutableMap.of("UNKNOWN", 0, "CONFIRMED", -50, "SUSPENDED", 100), userProfileService);

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }

    @Test
    public void ignoreEbayServiceExceptions() throws Exception {
        when(userProfileService.getUser(anyString())).thenThrow(new ServiceException("Test"));

        UserStateFilter userStateFilter = new UserStateFilter(ImmutableMap.of("UNKNOWN", 0, "CONFIRMED", -50, "SUSPENDED", 100), userProfileService);

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }
}
