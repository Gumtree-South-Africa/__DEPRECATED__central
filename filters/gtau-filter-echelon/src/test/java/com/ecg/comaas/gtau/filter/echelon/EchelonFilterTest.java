package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EchelonFilterTest {

    private static final String DEFAULT_RESPONSE_BODY = " KO ...";
    private static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put("ip", "127.0.0.1");
    }};

    @Mock
    private MessageProcessingContext contextMock;

    @Mock
    private Conversation conversationMock;

    @Mock
    private CloseableHttpClient httpClientMock;

    @Mock
    private CloseableHttpResponse httpResponseMock;

    @Mock
    private StatusLine statusLineMock;

    private EchelonFilter echelonFilter;

    @Before
    public void setUp() throws Exception {
        when(contextMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(contextMock.getConversation()).thenReturn(conversationMock);
        when(conversationMock.getMessages()).thenReturn(Collections.singletonList(mock(Message.class)));
        when(conversationMock.getCustomValues()).thenReturn(HEADERS);
        when(httpClientMock.execute(any(HttpGet.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponseMock.getEntity()).thenReturn(prepareWithBody(DEFAULT_RESPONSE_BODY));

        echelonFilter = new EchelonFilter(new EchelonFilterConfiguration("http://localhost", 1, 50), httpClientMock);
    }

    @Test
    public void whenIncorrectDirection_shouldReturnEmptyList() {
        when(contextMock.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenConversationNull_shouldReturnEmptyList() {
        when(contextMock.getConversation()).thenReturn(null);
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenMessagesNull_shouldReturnEmptyList() {
        when(conversationMock.getMessages()).thenReturn(null);
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenNoIpHeader_shouldReturnEmptyList() {
        when(conversationMock.getCustomValues()).thenReturn(Collections.emptyMap());
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenHttpCallFailed_shouldReturnEmptyList() throws IOException {
        when(httpClientMock.execute(any(HttpGet.class))).thenThrow(new IOException());
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenHttpStatusCodeNotOk_shouldReturnEmptyList() throws IOException {
        when(statusLineMock.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenHttpResponseBodyNull_shouldReturnEmptyList() throws IOException {
        when(httpResponseMock.getEntity()).thenReturn(null);
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenHttpResponseBodyHasNoKO_shouldReturnEmptyList() throws IOException {
        when(httpResponseMock.getEntity()).thenReturn(prepareWithBody("bla"));
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenHttpResponseBodyHasKO_shouldReturnDroppedOutcome() throws IOException {
        List<FilterFeedback> actualFeedback = echelonFilter.filter(contextMock);
        assertThat(actualFeedback).hasSize(1);
        assertThat(actualFeedback.get(0)).isEqualToComparingFieldByField(
                new FilterFeedback("Echelon", "...", 50, FilterResultState.DROPPED));
    }

    private static HttpEntity prepareWithBody(String body) {
        InputStream is = new ByteArrayInputStream(body.getBytes());
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(is);
        httpEntity.setContentLength(body.length());
        return httpEntity;
    }
}
