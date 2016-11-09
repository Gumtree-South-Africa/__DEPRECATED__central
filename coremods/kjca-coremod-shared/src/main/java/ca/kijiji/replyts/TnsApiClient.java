package ca.kijiji.replyts;


import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.Charset;

import static ca.kijiji.replyts.CloseableHttpClientBuilder.aCloseableHttpClient;


@Component
public class TnsApiClient {
    private final String baseUrl;
    private final String endPoint;
    private final String authHeader;
    private final CloseableHttpClient httpClient;

    @Autowired
    public TnsApiClient(
            @Value("${tnsapi.client.baseUrl:http://localhost:8080}") final String baseUrl,
            @Value("${tnsapi.client.endpoint:/tns/api/replier}") final String endPoint,
            @Value("${tnsapi.client.username:replyts}") final String username,
            @Value("${tnsapi.client.password:replyts}") final String password,
            @Value("${tnsapi.client.max.retries:1}") final Integer maxRetries,
            @Value("${tnsapi.client.timeout.socket.millis:400}") final Integer socketTimeout,
            @Value("${tnsapi.client.timeout.connect.millis:50}") final Integer connectTimeout,
            @Value("${tnsapi.client.timeout.connectionRequest.millis:300}") final Integer connectionRequestTimeout) {

        this.baseUrl = baseUrl;
        this.endPoint = endPoint;

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
            .build();
    }

    public void incrementReplyCount(String adId) throws Exception{
        URI uri = new URIBuilder(baseUrl).setPath(endPoint + "/ad/" + adId + "/increment-reply-count").build();
        new IncrementReplyCountCommand(httpClient, uri, authHeader).execute();
    }
}
