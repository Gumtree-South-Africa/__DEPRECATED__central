package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.*;
import ca.kijiji.tracing.TraceLogFilter;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.SendResponsivenessToServiceCommand.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SendResponsivenessToServiceCommandTest {
    private static final long USER_ID = 1L;
    private static final int TTR_S = 10;
    private static final String TRACE_VALUE = "trace-number";

    @Mock
    private ca.kijiji.discovery.ServiceDirectory serviceDirectory;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;

    @Captor
    private ArgumentCaptor<HttpHost> httpHostArgumentCaptor;
    @Captor
    private ArgumentCaptor<HttpPost> httpPostRequestArgumentCaptor;

    private SendResponsivenessToServiceCommand command;
    private LookupResult lookupResult;
    private StatusLine successStatusLine;

    @Before
    public void setUp() throws Exception {
        command = new SendResponsivenessToServiceCommand(serviceDirectory, httpClient, TRACE_VALUE, USER_ID, TTR_S, true);
        lookupResult = new LookupResult(ImmutableList.of(new ServiceEndpoint("host1", 3000), new ServiceEndpoint("host2", 3001)));
        successStatusLine = new BasicStatusLine(new ProtocolVersion("http", 1, 1), 204, "No Content");

        doReturn(lookupResult).when(serviceDirectory).lookup(any(SelectAll.class), eq(new LookupRequest(SERVICE_NAME, HTTP)));
        doReturn(httpResponse).when(httpClient).execute(any(HttpHost.class), any(HttpPost.class));
        doReturn(new BasicHttpEntity()).when(httpResponse).getEntity();
        doReturn(successStatusLine).when(httpResponse).getStatusLine();
    }

    @Test
    public void everythingWorkedOnTheFirstTry() throws Exception {
        command.run();

        verify(httpClient).execute(httpHostArgumentCaptor.capture(), httpPostRequestArgumentCaptor.capture());
        assertThat(httpHostArgumentCaptor.getValue().getHostName(), equalTo("host1"));
        assertThat(httpHostArgumentCaptor.getValue().getPort(), equalTo(3000));
        assertThat(httpPostRequestArgumentCaptor.getValue().getFirstHeader(TraceLogFilter.TRACE_HEADER).getValue(), equalTo(TRACE_VALUE));
        assertThat(httpPostRequestArgumentCaptor.getValue().getURI().getPath(), equalTo(ENDPOINT));

        verifyRequestEntity();
    }

    @Test
    public void discoveryFailsOnce_retried() throws Exception {
        doThrow(DiscoveryFailedException.class).doReturn(lookupResult)
                .when(serviceDirectory).lookup(any(SelectAll.class), eq(new LookupRequest(SERVICE_NAME, HTTP)));

        command.run();

        verify(httpClient).execute(httpHostArgumentCaptor.capture(), httpPostRequestArgumentCaptor.capture());
        assertThat(httpHostArgumentCaptor.getValue().getHostName(), equalTo("host1"));
        assertThat(httpHostArgumentCaptor.getValue().getPort(), equalTo(3000));
        assertThat(httpPostRequestArgumentCaptor.getValue().getFirstHeader(TraceLogFilter.TRACE_HEADER).getValue(), equalTo(TRACE_VALUE));
        assertThat(httpPostRequestArgumentCaptor.getValue().getURI().getPath(), equalTo(ENDPOINT));

        verifyRequestEntity();
    }

    @Test(expected = DiscoveryFailedException.class)
    public void discoveryFailsTwice_notRetried() throws Exception {
        doThrow(DiscoveryFailedException.class)
                .when(serviceDirectory).lookup(any(SelectAll.class), eq(new LookupRequest(SERVICE_NAME, HTTP)));

        command.run();
    }

    @Test
    public void firstEndpointFails_secondOneTried() throws Exception {
        doThrow(IOException.class).when(httpClient).execute(eq(new HttpHost("host1", 3000)), any(HttpPost.class));

        command.run();

        verify(httpClient, Mockito.times(2)).execute(httpHostArgumentCaptor.capture(), httpPostRequestArgumentCaptor.capture());
        assertThat(httpHostArgumentCaptor.getValue().getHostName(), equalTo("host2"));
        assertThat(httpHostArgumentCaptor.getValue().getPort(), equalTo(3001));
        assertThat(httpPostRequestArgumentCaptor.getValue().getFirstHeader(TraceLogFilter.TRACE_HEADER).getValue(), equalTo(TRACE_VALUE));
        assertThat(httpPostRequestArgumentCaptor.getValue().getURI().getPath(), equalTo(ENDPOINT));

        verifyRequestEntity();
    }

    @Test(expected = RuntimeException.class, timeout = 2000)
    public void secondEndpointFails_abortWithException() throws Exception {
        doThrow(IOException.class).when(httpClient).execute(eq(new HttpHost("host1", 3000)), any(HttpPost.class));
        doThrow(IOException.class).when(httpClient).execute(eq(new HttpHost("host2", 3001)), any(HttpPost.class));

        command.run();
    }

    @Test
    public void errorInResponse_retried() throws Exception {
        StatusLine serverError = new BasicStatusLine(new ProtocolVersion("http", 1, 1), 500, "Server error");

        doReturn(httpResponse).when(httpClient).execute(any(HttpHost.class), any(HttpPost.class));
        doReturn(serverError).doReturn(successStatusLine).when(httpResponse).getStatusLine();

        command.run();

        verify(httpClient, times(2)).execute(httpHostArgumentCaptor.capture(), httpPostRequestArgumentCaptor.capture());
        assertThat(httpHostArgumentCaptor.getValue().getHostName(), equalTo("host2"));
        assertThat(httpHostArgumentCaptor.getValue().getPort(), equalTo(3001));
        assertThat(httpPostRequestArgumentCaptor.getValue().getFirstHeader(TraceLogFilter.TRACE_HEADER).getValue(), equalTo(TRACE_VALUE));
        assertThat(httpPostRequestArgumentCaptor.getValue().getURI().getPath(), equalTo(ENDPOINT));

        verifyRequestEntity();
    }

    private void verifyRequestEntity() throws IOException {
        HttpEntity entity = httpPostRequestArgumentCaptor.getValue().getEntity();
        String requestJson = EntityUtils.toString(entity);
        DocumentContext documentContext = JsonPath.parse(requestJson);
        Long uid = documentContext.read("$.uid", Long.class);
        Integer timeToRespond = documentContext.read("$.ttr_s");
        assertThat(uid, equalTo(USER_ID));
        assertThat(timeToRespond, equalTo(TTR_S));
        assertThat(ContentType.get(entity).getMimeType(), equalTo("application/json"));
    }
}
