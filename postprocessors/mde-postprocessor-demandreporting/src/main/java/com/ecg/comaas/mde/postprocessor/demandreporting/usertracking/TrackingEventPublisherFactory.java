package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;


import org.apache.http.client.HttpClient;

import java.util.concurrent.BlockingQueue;

public class TrackingEventPublisherFactory {

    private final UserTrackingHandler.Config config;
    private HttpClient httpClient;

    public TrackingEventPublisherFactory(UserTrackingHandler.Config config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public Runnable create(BlockingQueue<TrackingEvent> queue) {
        return new TrackingEventPublisher(queue, config, httpClient);
    }
}
