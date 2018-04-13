package com.ecg.comaas.kjca.listener.userbehaviour.reporter.service;

import ca.kijiji.tracing.TraceLogFilter;
import com.ecg.comaas.kjca.listener.userbehaviour.model.ResponsivenessRecord;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.service.exception.HttpRequestFailedException;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.service.exception.IncorrectHttpStatusCodeException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.netflix.hystrix.HystrixCommand;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendResponsivenessToServiceCommandTest {

    private static final String DEFAULT_CONVERSATION_ID = "c1";
    private static final String DEFAULT_MESSAGE_ID = "m1";
    private static final long DEFAULT_USER_ID = 1L;
    private static final int DEFAULT_SECONDS_TO_RESPOND = 10;
    private static final HttpHost DEFAULT_HOST = new HttpHost("localhost", 80);

    private SendResponsivenessToServiceCommand objectUnderTest;

    @Mock
    private CloseableHttpClient httpClientMock;

    private final HystrixCommand.Setter setter = HystrixCommandConfigurationProvider.provideUserBehaviourConfig(true);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        objectUnderTest = new SendResponsivenessToServiceCommand(httpClientMock, setter, DEFAULT_HOST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNoRecord_shouldThrowException() throws Exception {
        objectUnderTest.run();
    }

    @Test(expected = HttpRequestFailedException.class)
    public void whenHttpRequestFails_shouldThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        objectUnderTest.run();
    }

    @Test
    public void whenIncorrectHttpStatusCodeObtained_shouldThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        configureHttpResponse(DEFAULT_HOST, HttpStatus.SC_OK);

        assertThatExceptionOfType(HttpRequestFailedException.class)
                .isThrownBy(() -> {
                    objectUnderTest.run();
                })
                .withRootCauseExactlyInstanceOf(IncorrectHttpStatusCodeException.class);
    }

    @Test
    public void whenCorrectHttpStatusCodeObtained_shouldNotThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        configureHttpResponse(DEFAULT_HOST, HttpStatus.SC_NO_CONTENT);

        objectUnderTest.run();
    }

    @Test
    public void verifyHttpRequest() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        configureHttpResponse(DEFAULT_HOST, HttpStatus.SC_NO_CONTENT);

        objectUnderTest.run();

        ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClientMock).execute(eq(DEFAULT_HOST), httpPostCaptor.capture());
        verifyHttpRequest(httpPostCaptor);
    }

    private static ResponsivenessRecord getDefaultRecord() {
        return new ResponsivenessRecord(1, DEFAULT_USER_ID, DEFAULT_CONVERSATION_ID, DEFAULT_MESSAGE_ID, DEFAULT_SECONDS_TO_RESPOND, Instant.now());
    }

    private void configureHttpResponse(HttpHost httpHost, int status) throws IOException {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = mock(StatusLine.class);

        when(responseMock.getEntity()).thenReturn(mock(HttpEntity.class));
        when(responseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(status);
        when(httpClientMock.execute(eq(httpHost), any(HttpPost.class))).thenReturn(responseMock);
    }

    private void verifyHttpRequest(ArgumentCaptor<HttpPost> httpPostCaptor) throws IOException {
        HttpPost actualRequest = httpPostCaptor.getValue();
        Header traceHeader = actualRequest.getFirstHeader(TraceLogFilter.TRACE_HEADER);
        assertThat(traceHeader.getValue()).isEqualTo(DEFAULT_CONVERSATION_ID + "/" + DEFAULT_MESSAGE_ID);

        HttpEntity entity = actualRequest.getEntity();
        assertThat(ContentType.get(entity).getMimeType()).isEqualTo(ContentType.APPLICATION_JSON.getMimeType());

        String requestJson = EntityUtils.toString(actualRequest.getEntity());
        DocumentContext documentContext = JsonPath.parse(requestJson);
        Long uid = documentContext.read("$.uid", Long.class);
        Integer timeToRespond = documentContext.read("$.ttr_s");
        assertThat(uid).isEqualTo(DEFAULT_USER_ID);
        assertThat(timeToRespond).isEqualTo(DEFAULT_SECONDS_TO_RESPOND);
    }
}
