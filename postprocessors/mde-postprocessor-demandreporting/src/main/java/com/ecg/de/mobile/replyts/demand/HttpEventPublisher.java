package com.ecg.de.mobile.replyts.demand;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import com.google.gson.Gson;
import de.mobile.analytics.domain.Event;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

public class HttpEventPublisher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(HttpEventPublisher.class);
    private final HttpClient httpClient;
    private final BehaviorTrackingHandler.Config config;
    private final Gson gson;
    private final BlockingQueue<Event> eventQueue;
    private final List<Event> buffer;

    HttpEventPublisher(BlockingQueue<Event> queue,
                       BehaviorTrackingHandler.Config config,
                       HttpClient httpClient,
                       Gson gson) {
        this.config = config;
        this.httpClient = httpClient;
        this.gson = gson;
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
        try {
            HttpPut request = prepareHttpCall();
            logger.debug("Sending request to event collector: " + request);
            httpClient.execute(request, SuccessStatusCodeResponseHandler.INSTANCE);
        } catch (IOException e) {
            logger.warn("Unable to send tracking events. {}", e);
        }
    }

    private HttpPut prepareHttpCall() {
        HttpPut method = new HttpPut(createEventEndpointUrl(config.getApiUrl()));
        String json = gson.toJson(buffer);
        logger.debug("Prepared Entity to send: " + json);
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
        public Boolean handleResponse(HttpResponse response) throws IOException {
            StatusLine statusLine = response.getStatusLine();

            if (statusLine == null) {
                logger.warn("Invalid Response statusLine null");
                return FALSE;
            }
            if (statusLine.getStatusCode() >= 200
                && statusLine.getStatusCode() < 300) {
                return TRUE;
            }
            logger.warn("Failed response status code {}", statusLine.getStatusCode());
            return FALSE;
        }

    }

}
