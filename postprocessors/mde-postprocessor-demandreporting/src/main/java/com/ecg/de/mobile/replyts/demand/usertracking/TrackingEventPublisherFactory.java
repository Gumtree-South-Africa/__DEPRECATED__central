package com.ecg.de.mobile.replyts.demand.usertracking;


import java.util.concurrent.BlockingQueue;

import org.apache.http.client.HttpClient;

import com.google.gson.Gson;


public class TrackingEventPublisherFactory {

    private final UserTrackingHandler.Config config;
    private final Gson gson;
    private HttpClient httpClient;

    public TrackingEventPublisherFactory(UserTrackingHandler.Config config, HttpClient httpClient, Gson gson) {
        this.config = config;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public Runnable create(BlockingQueue<TrackingEvent> queue) {
        return new TrackingEventPublisher(queue, config, httpClient, gson);
    }


}
