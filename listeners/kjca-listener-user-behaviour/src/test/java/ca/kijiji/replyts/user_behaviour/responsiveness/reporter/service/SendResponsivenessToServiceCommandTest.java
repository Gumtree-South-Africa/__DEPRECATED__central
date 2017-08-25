package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.ServiceEndpoint;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.tracing.TraceLogFilter;
import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.retry.RetryException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommand;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SendResponsivenessToServiceCommandTest.TestConfiguration.class)
public class SendResponsivenessToServiceCommandTest {

    private static final String DEFAULT_CONVERSATION_ID = "c1";
    private static final String DEFAULT_MESSAGE_ID = "m1";
    private static final long DEFAULT_USER_ID = 1L;
    private static final int DEFAULT_SECONDS_TO_RESPOND = 10;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;

    private SendResponsivenessToServiceCommand objectUnderTest;

    @Mock
    private EndpointDiscoveryService endpointDiscoveryServiceMock;

    @Mock
    private CloseableHttpClient httpClientMock;

    @Autowired
    @InjectMocks
    private HystrixCommand.Setter setter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        objectUnderTest = new SendResponsivenessToServiceCommand(endpointDiscoveryServiceMock, httpClientMock, setter);
    }

    @Test(expected = RuntimeException.class)
    public void whenNoEndpoints_shouldThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Collections.emptyList());

        objectUnderTest.run();
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNoRecord_shouldThrowException() throws Exception {
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Collections.emptyList());

        objectUnderTest.run();
    }

    @Test(expected = RetryException.class)
    @SuppressWarnings("unchecked")
    public void whenDiscoveryFailed_shouldThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenThrow(RetryException.class);

        objectUnderTest.run();
    }

    @Test(expected = RuntimeException.class)
    public void whenSingleEndpointFailed_shouldThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Collections.singletonList(new ServiceEndpoint(DEFAULT_HOST, DEFAULT_PORT)));

        objectUnderTest.run();
    }

    @Test(expected = RuntimeException.class)
    public void whenSingleEndpointResponseStatusIncorrect_shouldThrowException() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Collections.singletonList(new ServiceEndpoint(DEFAULT_HOST, DEFAULT_PORT)));
        configureHttpResponse(DEFAULT_HOST, DEFAULT_PORT, HttpStatus.SC_OK);

        objectUnderTest.run();
    }

    @Test
    public void whenSingleEndpointSucceeded_shouldSucceed() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Collections.singletonList(new ServiceEndpoint(DEFAULT_HOST, DEFAULT_PORT)));
        configureHttpResponse(DEFAULT_HOST, DEFAULT_PORT, HttpStatus.SC_NO_CONTENT);

        assertThat(objectUnderTest.run()).isNull();
    }

    @Test
    public void whenFirstEndpointFailed_shouldTrySecondEndpoint() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Arrays.asList(
                new ServiceEndpoint("fake_host", 7777),
                new ServiceEndpoint(DEFAULT_HOST, DEFAULT_PORT))
        );
        configureHttpResponse(DEFAULT_HOST, DEFAULT_PORT, HttpStatus.SC_NO_CONTENT);

        assertThat(objectUnderTest.run()).isNull();
    }

    @Test
    public void verifyHttpRequest() throws Exception {
        objectUnderTest.setResponsivenessRecord(getDefaultRecord());
        when(endpointDiscoveryServiceMock.discoverEndpoints()).thenReturn(Collections.singletonList(new ServiceEndpoint(DEFAULT_HOST, DEFAULT_PORT)));
        configureHttpResponse(DEFAULT_HOST, DEFAULT_PORT, HttpStatus.SC_NO_CONTENT);

        objectUnderTest.run();

        ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClientMock).execute(eq(new HttpHost(DEFAULT_HOST, DEFAULT_PORT)), httpPostCaptor.capture());
        verifyHttpRequest(httpPostCaptor);
    }

    private static ResponsivenessRecord getDefaultRecord() {
        return new ResponsivenessRecord(1, DEFAULT_USER_ID, DEFAULT_CONVERSATION_ID, DEFAULT_MESSAGE_ID, DEFAULT_SECONDS_TO_RESPOND, Instant.now());
    }

    private void configureHttpResponse(String host, int port, int status) throws IOException {
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = mock(StatusLine.class);

        when(responseMock.getEntity()).thenReturn(mock(HttpEntity.class));
        when(responseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(status);
        when(httpClientMock.execute(eq(new HttpHost(host, port)), any(HttpPost.class))).thenReturn(responseMock);
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

    @Configuration
    static class TestConfiguration {

        @Bean
        @Qualifier("userBehaviourHystrixConfig")
        public HystrixCommand.Setter userBehaviourHystrixConfig() {
            return HystrixCommandConfigurationProvider.provideUserBehaviourConfig(true);
        }
    }
}
