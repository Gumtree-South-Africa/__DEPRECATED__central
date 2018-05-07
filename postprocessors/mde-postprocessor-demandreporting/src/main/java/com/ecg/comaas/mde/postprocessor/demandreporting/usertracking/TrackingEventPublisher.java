package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.ecg.comaas.mde.postprocessor.demandreporting.GsonConfigurer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

class TrackingEventPublisher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TrackingEventPublisher.class);
    private final HttpClient httpClient;
    private final UserTrackingHandler.Config config;
    private final BlockingQueue<TrackingEvent> eventQueue;
    private final List<TrackingEvent> buffer;

    TrackingEventPublisher(BlockingQueue<TrackingEvent> queue,
                           UserTrackingHandler.Config config,
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
        try {
            HttpPut put = prepareHttpCall();
            httpClient.execute(put, SuccessStatusCodeResponseHandler.INSTANCE);
        } catch (IOException e) {
            logger.warn("Unable to send tracking events. {}", e.getMessage());
        }
    }

    private HttpPut prepareHttpCall() {
        HttpPut method = new HttpPut(createEventEndpointUrl(config.getApiUrl()));
        String json = GsonConfigurer.instance().toJson(buffer);
        logger.debug("Prepared Entity to send: " + json);
        method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return method;
    }

    private String createEventEndpointUrl(String apiUrl) {
        String url = (apiUrl == null ? "" : apiUrl).trim();
        url = (url.endsWith("/") ? url : url + "/");
        return url + "track";
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
            logger.warn("Failed response status code {} with reason {} and error message {}",
                statusLine.getStatusCode(), statusLine.getReasonPhrase(), extractErrorMessage(response.getEntity())
            );
            return FALSE;
        }
    }

    private static String extractErrorMessage(HttpEntity entity) {
        if (entity == null) {
            logger.warn("Failed to read error message - no response entity");
        } else {
           try (InputStream i = entity.getContent()) {
                return CharStreams.toString(new InputStreamReader(i, Charsets.UTF_8));
            } catch (IOException e) {
                logger.warn("Failed to read error message because of: ", e);
            }
        }
        return "Unable to read error message";
    }
}
