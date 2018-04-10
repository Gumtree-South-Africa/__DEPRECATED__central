package com.ecg.comaas.mde.postprocessor.demandreporting;


import com.google.gson.Gson;
import de.mobile.analytics.domain.Event;
import org.apache.http.client.HttpClient;

import java.util.concurrent.BlockingQueue;

public class HttpEventPublisherFactory implements EventPublisherFactory {
    private final BehaviorTrackingHandler.Config config;
    private final Gson gson;
    private HttpClient httpClient;

    public HttpEventPublisherFactory(BehaviorTrackingHandler.Config config, HttpClient httpClient, Gson gson) {
        this.config = config;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public Runnable create(BlockingQueue<Event> queue) {
        return new HttpEventPublisher(queue, config, httpClient, gson);
    }
}
