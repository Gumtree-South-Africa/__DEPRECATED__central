package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.ecg.comaas.mde.postprocessor.demandreporting.domain.Event;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class HttpEventPublisher implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(HttpEventPublisher.class);
    private final HttpClient httpClient;
    private final BehaviorTrackingHandler.Config config;
    private final BlockingQueue<Event> eventQueue;
    private final List<Event> buffer;

    HttpEventPublisher(BlockingQueue<Event> queue,
                       BehaviorTrackingHandler.Config config,
                       HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.eventQueue = queue;
        this.buffer = new ArrayList<>(config.getEventBufferSize());
    }

    @Override
    public void run() {
        if (eventQueue.drainTo(buffer, config.getEventBufferSize()) > 0) {
            sendEvents();
        }
    }

    private void sendEvents() {
        sendHttpCall();
        buffer.clear();
    }

    private void sendHttpCall() {
        HttpPut request = null;
        try {
            request = prepareHttpCall();
            LOG.trace("Sending request to event collector: {}", request);
            final Boolean executionWasSuccessful = httpClient.execute(request, SuccessStatusCodeResponseHandler.INSTANCE);
            if (!executionWasSuccessful) {
                LOG.warn("Failed to send event to event collector for {}", request.getRequestLine());
            }
        } catch (IOException e) {
            LOG.warn("Failed to send tracking event. {}", Optional.ofNullable(request).map(HttpRequestBase::getRequestLine).orElse(null), e);
        }
    }

    private HttpPut prepareHttpCall() {
        HttpPut method = new HttpPut(createEventEndpointUrl(config.getApiUrl()));
        String json = GsonConfigurer.instance().toJson(buffer);
        LOG.trace("Prepared Entity to send: {}", json);
        method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return method;
    }

    private String createEventEndpointUrl(String apiUrl) {
        String url = (apiUrl == null ? "" : apiUrl).trim();
        url = (url.endsWith("/") ? url : url + "/");
        return url + "event";
    }

    private enum SuccessStatusCodeResponseHandler implements ResponseHandler<Boolean> {
        INSTANCE;

        @Override
        public Boolean handleResponse(HttpResponse response) {
            try {
                StatusLine statusLine = response.getStatusLine();

                if (statusLine == null) {
                    LOG.warn("Invalid Response statusLine null");
                    return FALSE;
                }
                if (statusLine.getStatusCode() >= 200
                        && statusLine.getStatusCode() < 300) {
                    return TRUE;
                }
                LOG.warn("Failed response status code {} and response {}", statusLine.getStatusCode(), entityAsText(response));
                return FALSE;
            } finally {
                if (response.getEntity() != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }

        }

        private String entityAsText(HttpResponse response) {
            return Optional.ofNullable(response.getEntity()).map(entity -> {
                try {
                    return EntityUtils.toString(entity);
                } catch (IOException exceptionToSkip) {
                    return StringUtils.EMPTY;
                }
            }).orElse(StringUtils.EMPTY);
        }
    }
}
