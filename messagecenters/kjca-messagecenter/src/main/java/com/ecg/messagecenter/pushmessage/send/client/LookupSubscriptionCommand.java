package com.ecg.messagecenter.pushmessage.send.client;

import com.ecg.messagecenter.pushmessage.send.model.Subscription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * Request details about an arbitrary number of SEND subscriptions
 */
public class LookupSubscriptionCommand extends FailureAwareCommand<List<Subscription>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SUBSCRIPTIONS_BASE_URL = "/subscriptions";

    private LookupSubscriptionCommand(HttpClient httpClient,
                                      HttpHost httpHost,
                                      Long id,
                                      Long userId,
                                      String deviceToken,
                                      SendClient.DeliveryService deliveryService,
                                      SendClient.NotificationType type,
                                      Boolean enabled,
                                      Locale locale,
                                      Long offset,
                                      Long limit) {
        super(httpClient, httpHost);

        try {
            URIBuilder subscriptionLookupUriBuilder = new URIBuilder(SUBSCRIPTIONS_BASE_URL);
            URI conversationUri = buildWithParametersIfPossible(subscriptionLookupUriBuilder, id, userId, deviceToken, deliveryService, type, enabled, locale, offset, limit);
            this.request = new HttpGet(conversationUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static class Builder {
        private HttpClient httpClient = null;
        private HttpHost httpHost = null;
        private Long id = null;
        private Long userId = null;
        private String deviceToken = null;
        private SendClient.DeliveryService deliveryService = null;
        private SendClient.NotificationType type = null;
        private Boolean enabled = null;
        private Locale locale = null;
        private Long offset = null;
        private Long limit = null;

        public Builder(HttpClient httpClient, HttpHost httpHost) {
            this.httpClient = httpClient;
            this.httpHost = httpHost;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder setDeviceToken(String deviceToken) {
            this.deviceToken = deviceToken;
            return this;
        }

        public Builder setDeliveryService(SendClient.DeliveryService deliveryService) {
            this.deliveryService = deliveryService;
            return this;
        }

        public Builder setType(SendClient.NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public Builder setOffset(Long offset) {
            this.offset = offset;
            return this;
        }

        public Builder setLimit(Long limit) {
            this.limit = limit;
            return this;
        }

        public LookupSubscriptionCommand build() {
            return new LookupSubscriptionCommand(httpClient, httpHost, id, userId, deviceToken, deliveryService, type, enabled, locale, offset, limit);
        }
    }

    private static URI buildWithParametersIfPossible(URIBuilder base, Long id, Long userId, String deviceToken, SendClient.DeliveryService deliveryService, SendClient.NotificationType type, Boolean enabled, Locale locale, Long page, Long limit) throws URISyntaxException {
        if (id != null) {
            base.addParameter("id", String.valueOf(id));
        }
        if (userId != null) {
            base.addParameter("userId", String.valueOf(userId));
        }
        if (StringUtils.isNotBlank(deviceToken)) {
            base.addParameter("deviceToken", deviceToken);
        }
        if (deliveryService != null) {
            base.addParameter("deliveryService", deliveryService.name());
        }
        if (type != null) {
            base.addParameter("type", type.name());
        }
        if (locale != null) {
            base.addParameter("locale", locale.toString());
        }
        if (enabled != null) {
            base.addParameter("enabled", enabled.toString());
        }
        if (limit != null && limit > 0) {
            base.addParameter("limit", String.valueOf(limit));

            if (page != null && page > 0) {
                base.addParameter("offset", String.valueOf(limit * page));
            }
        }

        final URI uri = base.build();
        if (SUBSCRIPTIONS_BASE_URL.equals(uri.toString())) {
            throw new IllegalArgumentException("Can't look up all subscriptions.");
        }
        return uri;
    }

    @Override
    protected List<Subscription> successCallback(InputStream responseContent) throws IOException {
        return objectMapper.readValue(responseContent, new TypeReference<List<Subscription>>() {
        });
    }

    @Override
    protected String exceptionMessageTemplate() {
        return "{} error occurred while looking up subscriptions.";
    }
}
