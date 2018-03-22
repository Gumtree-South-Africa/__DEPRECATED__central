package com.ecg.de.mobile.replyts.pushnotification;

import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
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
import java.util.Optional;

@Component
public class NotificationSender {
    private static final Logger LOG = LoggerFactory.getLogger(MdePushNotificationListener.class);
    private static final String MIME_TYPE_APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();
    private final static int CUSTOMER_NOT_ELIGIBLE_FOR_PUSH = 404;

    @Value("${push.notification.service.url:}")
    private String apiUrl;

    @Autowired
    private HttpClient httpClient;

    public void send(MdePushMessagePayload payload) {
        if (StringUtils.isEmpty(apiUrl)) {
            LOG.warn("Not sending push notification, apiUrl not configured.");
            return;
        }

        HttpPost request = null;
        try {
            request = post(payload);
            LOG.trace("Sending request to push notification service: {}", request);
            final Boolean executionSuccessful = httpClient.execute(request, SuccessStatusCodeResponseHandler.INSTANCE);
            if (!executionSuccessful) {
                LOG.warn("Failed to send request to push notification service for {}", request.getRequestLine());
            }
        } catch (Exception e) {
            LOG.error("Unable to send push notification: {}", Optional.ofNullable(request).map(HttpRequestBase::getRequestLine).orElse(null), e);
        }
    }

    private HttpPost post(MdePushMessagePayload payload) throws Exception {
        HttpPost post = new HttpPost(apiUrl);
        post.addHeader(HTTP.CONTENT_TYPE, MIME_TYPE_APPLICATION_JSON);
        post.setEntity(
                new StringEntity(
                        ObjectMapperConfigurer.getObjectMapper().writeValueAsString(payload),
                        ContentType.APPLICATION_JSON
                )
        );
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
                final int statusCode = statusLine.getStatusCode();
                if ((statusCode >= 200 && statusCode < 300) || statusCode == CUSTOMER_NOT_ELIGIBLE_FOR_PUSH) {
                    return true;
                }
                LOG.warn("Failed to send request to push notification service {}, body: {}",
                        statusCode,
                        EntityUtils.toString(response.getEntity(), Consts.UTF_8)
                );
                return false;
            } finally {
                if (response.getEntity() != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        }
    }
}