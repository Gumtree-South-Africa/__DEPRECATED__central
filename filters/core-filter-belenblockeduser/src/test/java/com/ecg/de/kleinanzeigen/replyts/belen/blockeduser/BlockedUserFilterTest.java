package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BlockedUserFilterTest {

    BlockedUserFilter filter;

    @Mock
    private JdbcTemplate tpl;


    private MessageProcessingContext ctx;


    @Before
    public void setUp() throws Exception {
        filter = new BlockedUserFilter(tpl);
        ctx = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getConversation().getUserIdFor(ConversationRole.Buyer)).thenReturn("foo@bar.com");
        when(ctx.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
    }

    @Test
    public void recognizesUserAsBlockedAndBlocksMessage() {
        when(tpl.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList("BLOCKED"));

        List<FilterFeedback> feedbacks = filter.filter(ctx);

        assertEquals(FilterResultState.DROPPED, feedbacks.get(0).getResultState());
    }

    @Test
    public void recognizesUserAsNotBlockedAndAllowsMessage() {
        when(tpl.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList("OTHER"));

        List<FilterFeedback> feedbacks = filter.filter(ctx);

        assertTrue(feedbacks.isEmpty());
    }

    @Test
    public void doesNotKnowUserAndAllowsMessage() throws Exception {
        when(tpl.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.emptyList());

        List<FilterFeedback> feedbacks = filter.filter(ctx);

        assertTrue(feedbacks.isEmpty());
    }

}
