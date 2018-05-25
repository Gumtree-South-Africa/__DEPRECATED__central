package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.codahale.metrics.Timer;
import com.ecg.comaas.gtuk.listener.statsnotifier.event.GAEvent;
import com.ning.http.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static java.net.URLEncoder.encode;

public class GoogleAnalyticsService implements AnalyticsService<GAEvent> {

    private static Logger logger = LoggerFactory.getLogger(com.gumtree.analytics.GoogleAnalyticsService.class);

    private final AsyncHttpClient asyncHttpClient;
    private final String trackingId;
    private final String gaHostURL;

    public GoogleAnalyticsService(AsyncHttpClient asyncHttpClient, String trackingId, String gaHostURL) {
        this.asyncHttpClient = asyncHttpClient;
        this.trackingId = trackingId;
        this.gaHostURL = gaHostURL;

    }

    @Override
    public void sendAsyncEvent(GAEvent event, Optional<Timer> timer) {
        try {
            Optional<Timer.Context> timerContext = timer.isPresent() ? Optional.of(timer.get().time()) : Optional.<Timer.Context>empty();
            String url = prepareUrl(event);
            Request request = new RequestBuilder()
                    .addHeader("Content-length", "0")
                    .setMethod("POST")
                    .setUrl(url)
                    .build();
            logger.debug(String.format("URL to post: %s", url));
            asyncHttpClient.executeRequest(request, new AsyncCompletionHandler<Integer>() {

                @Override
                public Integer onCompleted(Response response) throws Exception {
                    if(response.getStatusCode() >= 200 && response.getStatusCode() < 210){
                        if (timerContext.isPresent()) timerContext.get().stop();
                        logger.debug("Successful request to Google analytics");
                    } else {
                        logger.error(String.format("Google Analytics returned an error: %s: %s",
                                response.getStatusCode(), response.getStatusText()));
                    }
                    return response.getStatusCode();
                }

                @Override
                public void onThrowable(Throwable t) {
                    logger.error(String.format("Error sending the Google Analytics request: %s", t.getMessage()));
                }
            });

        } catch (GAUriBuilderException e) {
            logger.error(String.format("Error creating the Google Analytics request: %s", e.getMessage()));
        }  catch (RuntimeException e) {
            logger.error(String.format("Error sending the Google Analytics request: %s", e.getMessage()));
        }
    }

    private String prepareUrl(GAEvent event) {
        GoogleAnalyticsURLBuilder builder = new GoogleAnalyticsURLBuilder()
                .withEC(event.getEventCategory())
                .withEA(event.getEventAction());
        if (event.getClientId().isPresent()) {
            builder.withCId(event.getClientId().get());
        }

        if (event.getCustomDimension().size() != 0) {
            builder.withCD(event.getCustomDimension());
        }

        return builder.build();
    }

    private class GoogleAnalyticsURLBuilder {

        private static final String SEPARATOR = "?";
        private String baseGaUrl = gaHostURL + SEPARATOR;
        private String fixPartQueryString = "v=1&t=event&tid=" + trackingId;

        private final HashMap<String, String> urlBuilder = new HashMap<>();

        public GoogleAnalyticsURLBuilder withEC(String eventConfiguration) {
            urlBuilder.put(GAURLValue.EVENT_CATEGORY.urlValue, eventConfiguration);
            return this;
        }

        public GoogleAnalyticsURLBuilder withEA(String eventAction) {
            urlBuilder.put(GAURLValue.EVENT_ACTION.urlValue, eventAction);
            return this;
        }

        public GoogleAnalyticsURLBuilder withCId(String clientId) {
            urlBuilder.put(GAURLValue.CLIENT_ID.urlValue, clientId);
            return this;
        }

        public GoogleAnalyticsURLBuilder withCD(HashMap<Integer, String> customDimensions) {
            customDimensions.entrySet().stream()
                .forEach(entry -> urlBuilder.put(join("", GAURLValue.CUSTOM_DIMENSION.urlValue, entry.getKey().toString()),
                                                entry.getValue()));
            return this;
        }

        public String build() {
            return new StringBuffer(baseGaUrl)
                        .append(fixPartQueryString)
                        .append(getDynamicQueryString()).toString();
        }

        private String getDynamicQueryString() {
            try {
                return urlBuilder.entrySet().stream().map(entry -> {
                    Assert.hasText(entry.getValue(), String.format("%s should be present", entry.getKey()));
                    try {
                        return join("", "&", entry.getKey(), "=", encode(entry.getValue(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new GAUriBuilderException("Unsupported encoding");
                    }
                }).collect(Collectors.joining());
            } catch (Exception e) {
                throw new GAUriBuilderException(e.getMessage());
            }
        }
    }

    private enum GAURLValue {
        EVENT_CATEGORY("ec"), EVENT_ACTION("ea"), CLIENT_ID("cid"), CUSTOM_DIMENSION("cd");
        private String urlValue;

        GAURLValue(String urlValue) {
            this.urlValue = urlValue;
        }

        public String getUrlValue() {
            return urlValue;
        }
    }
}
