package ca.kijiji.replyts;


import ca.kijiji.rsc.RemoteServiceException;
import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableList;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static ca.kijiji.replyts.CloseableHttpClientBuilder.aCloseableHttpClient;


@Component
public class TnsApiClient {
    private static final Logger LOG = LoggerFactory.getLogger(TnsApiClient.class);

    private final String baseUrl;
    private final String replierEndpoint;
    private final String authHeader;
    private final CloseableHttpClient httpClient;
    private final ImmutableList<URI> endpoints;

    @Autowired
    public TnsApiClient(
            @Value("${tnsapi.client.baseUrl:http://localhost:8080}") final String baseUrl,
            @Value("${tnsapi.client.endpoint:/tns/api/replier}") final String replierEndpoint,
            @Value("${tnsapi.client.username:replyts}") final String username,
            @Value("${tnsapi.client.password:replyts}") final String password,
            @Value("${tnsapi.client.max.retries:1}") final Integer maxRetries,
            @Value("${tnsapi.client.timeout.socket.millis:400}") final Integer socketTimeout,
            @Value("${tnsapi.client.timeout.connect.millis:50}") final Integer connectTimeout,
            @Value("${tnsapi.client.timeout.connectionRequest.millis:300}") final Integer connectionRequestTimeout) {

        this.baseUrl = baseUrl;
        this.replierEndpoint = replierEndpoint;
        this.endpoints = ImmutableList.of(URI.create(baseUrl + replierEndpoint));

        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
        this.authHeader = "Basic " + new String(encodedAuth);

        Counter retryCounter = TimingReports.newCounter("tnsapi.client.retries");
        DefaultHttpRequestRetryHandler retryHandler = new ZealousHttpRequestRetryHandler(maxRetries, retryCounter);

        this.httpClient = aCloseableHttpClient()
            .withHttpRequestRetryHandler(retryHandler)
            .withSocketTimeout(socketTimeout)
            .withConnectionTimeout(connectTimeout)
            .withConnectionManagerTimeout(connectionRequestTimeout)
            .withConnectionKeepAliveStrategy(new NetScalerDefaultConnKeepAliveStrategy())
            .withConnectionManager(new PoolingHttpClientConnectionManager())
            .build();
    }

    /**
     * Retrieves an arbitrary TnS API URL and converts the result to a Map
     *
     * @param path The URL path within
     * @return A map. If the request fails, the map will be empty.
     */
    @Nonnull
    public Map getJsonAsMap(final String path) {
        final HttpGet get = new HttpGet(this.baseUrl + "/api" + path);
        get.addHeader(HttpHeaders.AUTHORIZATION, this.authHeader);

        try {
            return new TnsFilterCheckCommand(this.httpClient, this.endpoints).withHttpRequest(get).execute();
        } catch (RemoteServiceException e) {
            LOG.warn("Encountered {} ({}) while looking up {} for map!", e.getCause().getClass().getSimpleName(), e.getCause().getMessage(), path);
            return Collections.emptyMap();
        }
    }

    /**
     * Bumps up the reply counter for the given ad
     *
     * This should only be called after the reply has successfully been sent to the poster, to ensure that the visible reply statistics reflect the number of
     * replies the poster is aware of.
     *
     * @param adId
     * @throws Exception
     */
    public void incrementReplyCount(final String adId) throws Exception {
        final URI uri = new URIBuilder(this.baseUrl).setPath(this.replierEndpoint + "/ad/" + adId + "/increment-reply-count").build();
        new IncrementReplyCountCommand(this.httpClient, uri, this.authHeader).execute();
    }
}
