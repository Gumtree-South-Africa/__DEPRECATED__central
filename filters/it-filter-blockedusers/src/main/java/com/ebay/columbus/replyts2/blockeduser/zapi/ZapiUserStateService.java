package com.ebay.columbus.replyts2.blockeduser.zapi;

import com.ebay.columbus.replyts2.blockeduser.UserStateService;
import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This implementation of UserStateService queries the data source through ZAPI
 * Created by ddallemule on 2/10/14.
 */
@Service
public class ZapiUserStateService implements UserStateService {

    private static final Logger LOG = LoggerFactory.getLogger(ZapiUserStateService.class);

    private final CloseableHttpClient httpClient;

    private String baseUrl;
    public static final String STATE = "status";
    public static final String BLACKLISTED_VALUE = "5";

    @Autowired
    public ZapiUserStateService(@Value("${zapi.hostname}") String baseUrl,
                                @Value("${api.connectionTimeout:1500}") Integer connectionTimeout,
                                @Value("${api.connectionManagerTimeout:1500}") Integer connectionManagerTimeout,
                                @Value("${api.socketTimeout:2500}") Integer socketTimeout,
                                @Value("${api.maxConnectionsPerHost:40}") Integer maxConnectionsPerHost,
                                @Value("${api.maxConnectionsPerHost:40}") Integer maxTotalConnections) {
        this.baseUrl = baseUrl;
        this.httpClient =
                buildHttpClient(connectionTimeout, connectionManagerTimeout, socketTimeout,
                        maxConnectionsPerHost, maxTotalConnections);
    }

    @Override
    public boolean isBlocked(String userEmail) throws Exception {
        return httpClient.execute(new HttpHost(baseUrl), createRequestMethod(userEmail),
                new UserStateResponseHandler(), new BasicHttpContext());
    }

    private HttpGet createRequestMethod(String userEmail) {
        HttpGet method = new HttpGet(String.format("/users/%s", userEmail));
        method.addHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
        method.addHeader(HttpHeaders.CONTENT_ENCODING, "UTF-8");
        return method;
    }

    public static CloseableHttpClient buildHttpClient(int connectionTimeout,
                                                      int connectionManagerTimeout, int socketTimeout, int maxConnectionsPerHost,
                                                      int maxTotalConnections) {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(
                createRequestConfig(connectionTimeout, connectionManagerTimeout,
                        socketTimeout));
        clientBuilder.setConnectionManager(
                createConnectionManager(maxConnectionsPerHost, maxTotalConnections));
        return clientBuilder.build();
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }

    private static RequestConfig createRequestConfig(int connectionTimeout,
                                                     int connectionManagerTimeout, int socketTimeout) {
        return RequestConfig.custom().setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .setConnectTimeout(connectionManagerTimeout).build();
    }

    private static PoolingHttpClientConnectionManager createConnectionManager(
            int maxConnectionsPerHost, int maxTotalConnections) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotalConnections);
        cm.setDefaultMaxPerRoute(maxConnectionsPerHost);
        return cm;
    }

    private class UserStateResponseHandler implements ResponseHandler<Boolean> {

        @Override
        public Boolean handleResponse(HttpResponse httpResponse) throws IOException {
            return checkStatusCode(httpResponse.getStatusLine().getStatusCode()) && parseResponse(
                    CharStreams.toString(new InputStreamReader(
                            httpResponse.getEntity().getContent(), "UTF-8")));
        }

        private boolean checkStatusCode(int statusCode) {
            LOG.trace("StatusCode: {}", statusCode);
            return statusCode < 400;
        }

        private boolean parseResponse(String payload) throws IOException {
            LOG.trace("Payload: {}", payload);
            JsonNode jsonNode = new ObjectMapper().readTree(payload);
            JsonNode stateValue = jsonNode.get(STATE);
            checkNotNull(stateValue, "Cannot find user state in message payload: %s", payload);
            return stateValue.asText().equalsIgnoreCase(BLACKLISTED_VALUE);
        }
    }
}
