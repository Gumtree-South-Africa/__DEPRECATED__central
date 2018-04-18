package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockedUserFilterTest {

    private BlockedUserFilter blockedUserFilter;

    @Mock
    private JdbcTemplate jdbcTemplateMock;

    @Mock
    private MessageProcessingContext contextMock;

    @Before
    public void setUp() throws Exception {
        Conversation conversationMock = mock(Conversation.class);
        when(conversationMock.getUserIdFor(any(ConversationRole.class))).thenReturn("user@example.com");
        when(contextMock.getConversation()).thenReturn(conversationMock);
        when(contextMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        blockedUserFilter = new BlockedUserFilter(jdbcTemplateMock, true);
    }

    @Test
    public void whenUserDataActive_shouldReturnEmptyList() {
        prepareUserData(UserState.ACTIVE.name());

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).isEmpty();
    }

    @Test
    public void whenUserDataBlocked_shouldReturnSingleFeedback() {
        prepareUserData(UserState.BLOCKED.name());

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).hasSize(1);
        assertThat(feedback.get(0)).isEqualToComparingFieldByField(blockedFeedback());
    }

    @Test
    public void whenUserDataEmpty_andExtTnsDisabled_shouldReturnEmptyList() {
        blockedUserFilter = new BlockedUserFilter(jdbcTemplateMock, false);
        when(jdbcTemplateMock.query(contains("userdata"), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.emptyList());

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).isEmpty();
    }

    @Test
    public void whenUserDataUndecided_andExtTnsDisabled_shouldReturnEmptyList() {
        blockedUserFilter = new BlockedUserFilter(jdbcTemplateMock, false);
        prepareUserData(UserState.UNDECIDED.name());

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).isEmpty();
    }

    @Test
    public void whenUserDataNull_andExtTnsNull_shouldReturnEmptyList() {
        prepareUserData(null);
        prepareExternalTnS(null);

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).isEmpty();
    }

    @Test
    public void whenUserDataUndecided_andExtTnsActive_shouldReturnEmptyList() {
        prepareUserData(UserState.UNDECIDED.name());
        prepareExternalTnS(UserState.ACTIVE.name());

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).isEmpty();
    }

    @Test
    public void whenUserDataUndecided_andExtTnsBlocked_shouldReturnSingleFeedback() {
        prepareUserData(UserState.UNDECIDED.name());
        prepareExternalTnS(UserState.BLOCKED.name());

        List<FilterFeedback> feedback = blockedUserFilter.filter(contextMock);

        assertThat(feedback).hasSize(1);
        assertThat(feedback.get(0)).isEqualToComparingFieldByField(blockedFeedback());
    }

    private void prepareUserData(String state) {
        when(jdbcTemplateMock.query(contains("userdata"), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.singletonList(state));
    }

    private void prepareExternalTnS(String state) {
        when(jdbcTemplateMock.query(contains("external_user_tns"), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.singletonList(state));
    }

    private static FilterFeedback blockedFeedback() {
        return new FilterFeedback("BLOCKED", "User is blocked user@example.com", 0, FilterResultState.DROPPED);
    }
}
