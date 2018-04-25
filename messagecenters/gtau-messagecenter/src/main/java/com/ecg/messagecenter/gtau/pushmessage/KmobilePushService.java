package com.ecg.messagecenter.gtau.pushmessage;

import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
public class KmobilePushService extends PushService {

    private final CloseableHttpClient httpClient;
    private final HttpHost pushHttpHost;

    public KmobilePushService(@Value("${push-mobile.host:}") String pushHost,
                              @Value("${push-mobile.port:80}") int pushPort,
                              @Value("${replyts2-messagecenter-plugin.pushmobile.timeout.connect.millis:4000}") int connectTimeout,
                              @Value("${replyts2-messagecenter-plugin.pushmobile.timeout.socket.millis:8000}") int socketTimeout,
                              @Value("${replyts2-messagecenter-plugin.pushmobile.timeout.connectionManager.millis:4000}") int connectionManagerTimeout,
                              @Value("${replyts2-messagecenter-plugin.pushmobile.maxConnectionsPerHost:40}") int maxConnectionsPerHost,
                              @Value("${replyts2-messagecenter-plugin.pushmobile.maxTotalConnections:40}") int maxTotalConnections) {
        this.pushHttpHost = new HttpHost(pushHost, pushPort);
        this.httpClient = HttpClientFactory.createCloseableHttpClient(connectTimeout, connectionManagerTimeout, socketTimeout, maxConnectionsPerHost, maxTotalConnections);
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }

    public Result sendPushMessage(final PushMessagePayload payload) {
        try {
            HttpRequest request = buildRequest(payload);
            return httpClient.execute(pushHttpHost, request, new ResponseHandler<Result>() {
                @Override
                public Result handleResponse(HttpResponse response) throws IOException {
                    int code = response.getStatusLine().getStatusCode();
                    switch (code) {
                        case 200:
                            return Result.ok(payload);
                        case 404:
                            return Result.notFound(payload);
                        default:
                            // application wise only 200 (sending success) + 404 (non-registered device) make sense to us
                            return Result.error(payload, new RuntimeException("Unexpected response: " + response.getStatusLine()));
                    }
                }
            });
        } catch (Exception e) {
            return Result.error(payload, e);
        }
    }

    private HttpRequest buildRequest(PushMessagePayload payload) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost("/push-mobile/messages");

        post.setEntity(new StringEntity(payload.asJson(), ContentType.create("application/json", "UTF-8")));
        // auth-setting via header much easier as doing yucky handling basic auth aspect in http-client builder
        post.setHeader("Authorization", "Basic a2Nyb246ZmQzMWxvcWE=");
        return post;
    }
}