package com.ecg.comaas.mde.postprocessor.demandreporting.client.rest.internal;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.Event;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.rest.DemandReportingHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RestApiWriter implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DemandReportingHttpClient httpClient;
    private final Event event;
    private final String baseUrl;

    public RestApiWriter(Event event, String baseUrl, DemandReportingHttpClient httpClient) {
        this.event = event;
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public void run() {
        String json = buildJson(event);
        String url = buildUrl(baseUrl, event);
        long start = System.currentTimeMillis();
        try {
            httpClient.postJson(url, json);
            long duration = System.currentTimeMillis() - start;
            logger.info("Posting demand event took {} ms.", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("Could not post demand event to " + url + ". Tried for " + duration + " ms. Content was " + json, e);
        }
    }

    private static String buildUrl(String baseUrl, Event event) {
        String url = baseUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += event.getEventType();
        return url;
    }

    private static String buildJson(Event event) {
        Map<String, String> eventProperties = new HashMap<String, String>();
        if (event.getPublisher() != null) {
            eventProperties.put("publisher", event.getPublisher());
        }
        if (event.getReferrer() != null) {
            eventProperties.put("referrer", event.getReferrer());
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"ad_id\":").append(event.getAdId()).append(", ");
        sb.append("\"customer_id\":").append(event.getCustomerId()).append(", ");

        sb.append("\"event_properties\":[{");
        for (Iterator<Entry<String, String>> i = eventProperties.entrySet().iterator(); i.hasNext(); ) {
            Entry<String, String> entry = i.next();
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}]}");

        return sb.toString();
    }

}
