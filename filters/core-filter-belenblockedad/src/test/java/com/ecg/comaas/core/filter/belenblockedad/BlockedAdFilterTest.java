package com.ecg.comaas.core.filter.belenblockedad;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockedAdFilterTest {

    BlockedAdFilter filter;

    @Mock
    private JdbcTemplate tpl;


    private MessageProcessingContext ctx;


    @Before
    public void setUp() throws Exception {
        filter = new BlockedAdFilter("filter", tpl);
        ctx = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(ctx.getConversation().getAdId()).thenReturn("123");
    }

    @Test
    public void messageDropIfAdHasSpamFraudReason() {
        when(tpl.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList(BlockedAdFilter.SPAM_FRAUD_REASON));

        List<FilterFeedback> feedbacks = filter.filter(ctx);

        assertEquals(1, feedbacks.size());
        assertEquals(FilterResultState.DROPPED, feedbacks.get(0).getResultState());
    }

    @Test
    public void messageOKIfAdHasNoSpamFraudReason() {
        when(tpl.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList("DUPLICATE"));

        List<FilterFeedback> feedbacks = filter.filter(ctx);

        assertEquals(0, feedbacks.size());
    }

    @Test
    public void messageOKIfAdDoesNotExist() throws Exception {
        when(tpl.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.emptyList());

        List<FilterFeedback> feedbacks = filter.filter(ctx);

        assertEquals(0, feedbacks.size());
    }
}
