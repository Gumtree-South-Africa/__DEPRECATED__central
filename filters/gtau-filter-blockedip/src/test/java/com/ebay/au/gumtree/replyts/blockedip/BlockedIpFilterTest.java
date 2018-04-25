package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockedIpFilterTest {

    private static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put("X-Cust-Ip", "127.0.0.1");
    }};

    private BlockedIpFilter blockedIpFilter;

    @Mock
    private JdbcTemplate jdbcTemplateMock;

    @Mock
    private MessageProcessingContext contextMock;

    @Mock
    private Message messageMock;

    @Before
    public void setup() {
        when(messageMock.getHeaders()).thenReturn(HEADERS);
        when(contextMock.getMessage()).thenReturn(messageMock);
        blockedIpFilter = new BlockedIpFilter(jdbcTemplateMock);
    }

    @Test
    public void whenNoHeader_shouldReturnEmptyList() {
        when(messageMock.getHeaders()).thenReturn(Collections.emptyMap());

        List<FilterFeedback> actualFeedback = blockedIpFilter.filter(contextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenDbThrowsException_shouldReturnEmptyList() {
        when(jdbcTemplateMock.query(anyString(), any(RowMapper.class), any(Object[].class))).thenThrow(new DataAccessTestException());

        List<FilterFeedback> actualFeedback = blockedIpFilter.filter(contextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenDbReturnNull_shouldReturnEmptyList() {
        when(jdbcTemplateMock.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(null);

        List<FilterFeedback> actualFeedback = blockedIpFilter.filter(contextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenDbReturnNoResult_shouldReturnEmptyList() {
        when(jdbcTemplateMock.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.emptyList());

        List<FilterFeedback> actualFeedback = blockedIpFilter.filter(contextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenAlreadyExpired_shouldReturnEmptyList() {
        Timestamp fiveSecAgo = new Timestamp(System.currentTimeMillis() - 5_000);
        when(jdbcTemplateMock.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.singletonList(fiveSecAgo));

        List<FilterFeedback> actualFeedback = blockedIpFilter.filter(contextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenNotExpiredYet_shouldReturnSingleFeedback() {
        Timestamp oneMinFromNow = new Timestamp(System.currentTimeMillis() + 60_000);
        when(jdbcTemplateMock.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(Collections.singletonList(oneMinFromNow));

        List<FilterFeedback> actualFeedback = blockedIpFilter.filter(contextMock);

        assertThat(actualFeedback).hasSize(1);
        assertThat(actualFeedback.get(0)).isEqualToComparingFieldByField(
                new FilterFeedback("BLOCKED", "IP Address is blocked 127.0.0.1", 0, FilterResultState.DROPPED));
    }

    private class DataAccessTestException extends DataAccessException {
        public DataAccessTestException() {
            super("");
        }
    }
}
