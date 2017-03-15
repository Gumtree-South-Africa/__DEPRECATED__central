package com.ecg.messagecenter.pushmessage;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static com.ecg.messagecenter.pushmessage.HttpClientBuilder.buildHttpClient;

/**
 * User: maldana
 * Date: 18.10.13
 * Time: 11:14
 *
 * @author maldana@ebay.de
 */
@Component
public class PushService {

    private static final Logger LOG = LoggerFactory.getLogger(PushService.class);

    private final HttpClient httpClient;
    private final String pushMobileUrl;

    private static final String ENDPOINT_MESSAGES = "/messages";

    public PushService(@Value("${replyts2-messagecenter-plugin.pushmobile.url:http://push-mobile.service.kconsul}") String pushMobileUrl,
                       @Value("${replyts2-messagecenter-plugin.pushmobile.timeout.connect.millis:2000}") int connectTimeout,
                       @Value("${replyts2-messagecenter-plugin.pushmobile.timeout.socket.millis:4000}") int socketTimeout,
                       @Value("${replyts2-messagecenter-plugin.pushmobile.timeout.connectionManager.millis:2000}") int connectionManagerTimeout,
                       @Value("${replyts2-messagecenter-plugin.pushmobile.maxConnectionsPerHost:40}") int maxConnectionsPerHost,
                       @Value("${replyts2-messagecenter-plugin.pushmobile.maxTotalConnections:40}") int maxTotalConnections) {
        this.httpClient = buildHttpClient(connectTimeout, connectionManagerTimeout, socketTimeout, maxConnectionsPerHost, maxTotalConnections);
        this.pushMobileUrl = pushMobileUrl;
    }

    public Result sendPushMessage(final PushMessagePayload payload) {
        try {
            HttpUriRequest request = buildRequest(payload);
            return httpClient.execute(request, new ResponseHandler<Result>() {
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

    private HttpUriRequest buildRequest(PushMessagePayload payload) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(pushMobileUrl + ENDPOINT_MESSAGES);

        post.setEntity(new StringEntity(payload.asJson().toString(), ContentType.create("application/json", "UTF-8")));
        return post;
    }

    public static class Result {

        public enum Status {
            OK, NOT_FOUND, ERROR
        }

        private PushMessagePayload payload;
        private Status status;
        private Optional<Exception> e;

        private Result(PushMessagePayload payload, Status status, Optional<Exception> e) {
            this.payload = payload;
            this.status = status;
            this.e = e;
        }

        public static Result ok(PushMessagePayload payload) {
            return new Result(payload, Status.OK, Optional.<Exception>empty());
        }

        public static Result notFound(PushMessagePayload payload) {
            return new Result(payload, Status.NOT_FOUND, Optional.<Exception>empty());
        }

        public static Result error(PushMessagePayload payload, Exception e) {
            return new Result(payload, Status.ERROR, Optional.of(e));
        }

        public Status getStatus() {
            return status;
        }

        public PushMessagePayload getPayload() {
            return payload;
        }

        public Optional<Exception> getException() {
            return e;
        }
    }
}