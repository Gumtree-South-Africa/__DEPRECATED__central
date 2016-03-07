package com.ecg.de.mobile.replyts.demand;


import java.util.concurrent.BlockingQueue;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.mobile.analytics.domain.Event;

public class HttpEventPublisherFactory implements EventPublisherFactory {

    private static final Logger logger = LoggerFactory.getLogger(HttpEventPublisherFactory.class);
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
