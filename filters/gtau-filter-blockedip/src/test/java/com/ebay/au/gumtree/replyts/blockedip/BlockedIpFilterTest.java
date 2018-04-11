package com.ebay.au.gumtree.replyts.blockedip;

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

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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
public class BlockedIpFilterTest {
    @Mock
    private JdbcTemplate template;

    private MessageProcessingContext context;
    private BlockedIpFilter filter;

    @Before
    public void setup() {
        filter = new BlockedIpFilter(template);
        context = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(context.getMessage().getHeaders().get(BlockedIpFilter.IP_ADDR_HEADER)).thenReturn("127.0.0.1");
    }

    @Test
    public void dropMessageIfIpFoundAndNotExpired() {
        Timestamp expiry = Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(new java.util.Date().getTime() + 1000 * 1000 * 60)));
        when(template.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList(expiry));

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertEquals(1, feedbacks.size());
        assertEquals(FilterResultState.DROPPED, feedbacks.get(0).getResultState());
    }

    @Test
    public void passMessageIfIpFoundAndExpired() {
        Timestamp expiry = Timestamp.valueOf("2013-05-24 10:16:30.0");
        when(template.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList(expiry));

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertEquals(0, feedbacks.size());
    }

    @Test
    public void passMessageIfIpNotFound() {
        when(template.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.emptyList());

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertEquals(0, feedbacks.size());
    }

    @Test
    public void handlesMultipleRecords() {
        Timestamp valid = Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(new java.util.Date().getTime() + 1000 * 1000 * 60)));
        Timestamp expired = Timestamp.valueOf("2013-05-24 10:16:30.0");
        Timestamp old = Timestamp.valueOf("2000-05-24 10:16:30.0");

        when(template.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(asList(old, expired, valid));

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertEquals(1, feedbacks.size());
        assertEquals(FilterResultState.DROPPED, feedbacks.get(0).getResultState());
    }
}
