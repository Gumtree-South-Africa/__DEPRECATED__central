package com.ecg.de.ebayk.messagecenter.filters;

import com.ecg.de.ebayk.messagecenter.persistence.ConversationBlock;
import com.ecg.de.ebayk.messagecenter.persistence.ConversationBlockRepository;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserBlockedConversationFilterTest {
    public static final String CONV_ID = "cid";
    public static final int SCORE = 10_000;

    private UserBlockedConversationFilter filter;

    private ProcessingTimeGuard guard = new ProcessingTimeGuard(0);

    @Mock
    private ConversationBlockRepository conversationBlockRepository;
    @Mock
    private MutableConversation conversation;
    @Mock
    private Mail mail;
    private MessageProcessingContext mpc;

    @Before
    public void setUp() throws Exception {
        filter = new UserBlockedConversationFilter(conversationBlockRepository, SCORE);

        when(mail.makeMutableCopy()).thenReturn(null);
        when(conversation.getImmutableConversation()).thenReturn(conversation);
        when(conversation.getId()).thenReturn(CONV_ID);

        mpc = new MessageProcessingContext(mail, CONV_ID, guard);
        mpc.setConversation(conversation);
    }

    @Test
    public void buyerBlocked_dropped() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        ConversationBlock conversationBlock = new ConversationBlock(
                CONV_ID,
                ConversationBlock.LATEST_VERSION,
                Optional.empty(),
                Optional.of(now)
        );

        when(conversationBlockRepository.byConversationId(CONV_ID)).thenReturn(conversationBlock);

        List<FilterFeedback> feedbacks = filter.filter(mpc);

        assertThat(feedbacks, is(notNullValue()));
        assertThat(feedbacks.size(), equalTo(1));
        assertThat(feedbacks.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(feedbacks.get(0).getScore(), equalTo(SCORE));
        assertThat(feedbacks.get(0).getDescription(), equalTo(UserBlockedConversationFilter.DESC_SELLER_BLOCKED_BUYER));
        assertThat(feedbacks.get(0).getUiHint(), containsString(now.toString()));
    }

    @Test
    public void noOneBlocked_sent() throws Exception {
        when(conversationBlockRepository.byConversationId(CONV_ID)).thenReturn(null);

        List<FilterFeedback> feedbacks = filter.filter(mpc);

        assertThat(feedbacks, is(notNullValue()));
        assertThat(feedbacks.size(), equalTo(0));
    }

    @Test
    public void bothBlocked_doubleScore_dropped() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        ConversationBlock conversationBlock = new ConversationBlock(
                CONV_ID,
                ConversationBlock.LATEST_VERSION,
                Optional.of(now),
                Optional.of(now)
        );

        when(conversationBlockRepository.byConversationId(CONV_ID)).thenReturn(conversationBlock);

        List<FilterFeedback> feedbacks = filter.filter(mpc);

        assertThat(feedbacks, is(notNullValue()));
        assertThat(feedbacks.size(), equalTo(2));

        assertThat(feedbacks.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(feedbacks.get(0).getScore(), equalTo(SCORE));
        assertThat(feedbacks.get(0).getDescription(), equalTo(UserBlockedConversationFilter.DESC_BUYER_BLOCKED_SELLER));
        assertThat(feedbacks.get(0).getUiHint(), containsString(now.toString()));

        assertThat(feedbacks.get(1).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(feedbacks.get(1).getScore(), equalTo(SCORE));
        assertThat(feedbacks.get(1).getDescription(), equalTo(UserBlockedConversationFilter.DESC_SELLER_BLOCKED_BUYER));
        assertThat(feedbacks.get(1).getUiHint(), containsString(now.toString()));
    }
}
