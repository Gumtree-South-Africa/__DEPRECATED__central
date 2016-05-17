package com.ecg.messagecenter.pushmessage;

import com.google.common.base.Optional;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static com.ecg.messagecenter.pushmessage.HttpClientBuilder.buildHttpClient;

public class PushService {

    private static final Logger LOG = LoggerFactory.getLogger(PushService.class);

    private final HttpClient httpClient;
    private final HttpHost mobilepushHost;

    public PushService(String mobilepushUrl, Integer mobilepushPort) {
        this.httpClient = buildHttpClient(1000, 1000, 2000, 40, 40);
        this.mobilepushHost = new HttpHost(mobilepushUrl, mobilepushPort);
    }

    public Result sendPushMessage(final PushMessagePayload payload) {

        try {

            HttpRequest request = buildRequest(payload);
            return httpClient.execute(mobilepushHost, request, new ResponseHandler<Result>() {
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
            return new Result(payload, Status.OK, Optional.<Exception>absent());
        }

        public static Result notFound(PushMessagePayload payload) {
            return new Result(payload, Status.NOT_FOUND, Optional.<Exception>absent());
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
