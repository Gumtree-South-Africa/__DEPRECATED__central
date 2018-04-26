package com.ecg.messagecenter.it.pushmessage;

import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * User: maldana
 * Date: 18.10.13
 * Time: 11:14
 *
 * @author maldana@ebay.de
 */
public class KmobilePushService extends PushService {
    private final CloseableHttpClient httpClient;
    private final HttpHost kmobilepushHost;

    public KmobilePushService(String kmobilepushHost, Integer kmobilepushPort, String protocol) {
        this.httpClient = HttpClientFactory.createCloseableHttpClient(4000, 4000, 8000, 40, 40);
        this.kmobilepushHost = new HttpHost(kmobilepushHost, kmobilepushPort, protocol);
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }

    public Result sendPushMessage(final PushMessagePayload payload) {

        try {

            HttpRequest request = buildRequest(payload);
            return httpClient.execute(kmobilepushHost, request, new ResponseHandler<Result>() {
                @Override public Result handleResponse(HttpResponse response) throws IOException {
                    int code = response.getStatusLine().getStatusCode();
                    switch (code) {
                        case 200:
                            return Result.ok(payload);
                        case 404:
                            return Result.notFound(payload);
                        default:
                            // application wise only 200 (sending success) + 404 (non-registered device) make sense to us
                            return Result.error(payload, new RuntimeException(
                                            "Unexpected response: " + response.getStatusLine()));
                    }
                }
            });


        } catch (Exception e) {
            return Result.error(payload, e);
        }
    }

    private HttpRequest buildRequest(PushMessagePayload payload)
                    throws UnsupportedEncodingException {
        HttpPost post = new HttpPost("/push-mobile/messages");

        post.setEntity(new StringEntity(payload.asJson(),
                        ContentType.create("application/json", "UTF-8")));
        // auth-setting via header much easier as doing yucky handling basic auth aspect in http-client builder
        post.setHeader("Authorization", "Basic a2Nyb246ZmQzMWxvcWE=");
        return post;
    }

}
