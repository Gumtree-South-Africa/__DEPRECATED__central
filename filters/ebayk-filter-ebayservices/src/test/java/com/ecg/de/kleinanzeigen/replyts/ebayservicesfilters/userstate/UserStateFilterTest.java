package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ebay.marketplace.user.v1.services.MemberBadgeDataType;
import com.ebay.marketplace.user.v1.services.UserEnum;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

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
    private UserService userService;
    @Mock
    private MemberBadgeDataType badgeData;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext mpc;

    private UserStateFilter userStateFilter;

    @Before
    public void setUp() throws Exception {
        when(mpc.getMail().getFrom()).thenReturn("sender@test.de");
        when(userService.getMemberBadgeData(anyString())).thenReturn(badgeData);

        userStateFilter = new UserStateFilter(ImmutableMap.of("UNKNOWN", 0, "CONFIRMED", -50, "SUSPENDED", 100), userService);
    }

    @Test
    public void rateBadUser() throws Exception {
        when(badgeData.getUserState()).thenReturn(UserEnum.SUSPENDED);

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
        when(badgeData.getUserState()).thenReturn(UserEnum.UNKNOWN);

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }

    @Test
    public void ignoreEmptyUserState() throws Exception {
        when(badgeData.getUserState()).thenReturn(null);

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }

    @Test
    public void ignoreEbayServiceExceptions() throws Exception {
        when(userService.getMemberBadgeData(anyString())).thenThrow(new ServiceException("Test"));

        List<FilterFeedback> feedbacks = userStateFilter.filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }
}
