package com.ecg.de.mobile.replyts.pushnotification;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificationSender {
    private static final Logger LOG = LoggerFactory.getLogger(MdePushNotificationListener.class);

    @Value("${push.notification.service.url:}")
    private String apiUrl;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private JsonConverter jsonConverter;

    public void send(MdePushMessagePayload payload) {
        if (StringUtils.isEmpty(apiUrl)) {
            LOG.warn("Not sending push notification, apiUrl not configured.");
            return;
        }

        try {
            HttpPost request = post(payload);
            LOG.debug("Sending request to push notification service: {}", request);
            httpClient.execute(request, SuccessStatusCodeResponseHandler.INSTANCE);
        } catch (Exception e) {
            LOG.error("Unable to send push notification: {}", e.getMessage(), e);
        }
    }

    private HttpPost post(MdePushMessagePayload payload) throws Exception {
        StringEntity params = new StringEntity(
                jsonConverter.toJsonString(payload), ContentType.APPLICATION_JSON
        );
        HttpPost post = new HttpPost(apiUrl);
        post.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(params);
        return post;
    }

    private enum SuccessStatusCodeResponseHandler implements ResponseHandler<Boolean> {
        INSTANCE;

        @Override
        public Boolean handleResponse(HttpResponse response) throws IOException {
            try {
                StatusLine statusLine = response.getStatusLine();

                if (statusLine == null) {
                    LOG.warn("Invalid Response statusLine null");
                    return false;
                }
                if (200 <= statusLine.getStatusCode() && statusLine.getStatusCode() < 400) {
                    return true;
                }
                LOG.warn("Failed response status code {}, body: {}",
                        statusLine.getStatusCode(),
                        EntityUtils.toString(response.getEntity(), Consts.UTF_8)
                );
                return false;
            } finally {
                if (response.getEntity() != null) {
                    EntityUtils.consume(response.getEntity());
                }
            }
        }
    }
}