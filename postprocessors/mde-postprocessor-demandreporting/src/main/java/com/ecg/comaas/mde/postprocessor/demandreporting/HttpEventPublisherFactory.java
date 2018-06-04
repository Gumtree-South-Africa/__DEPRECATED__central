package com.ecg.comaas.mde.postprocessor.demandreporting;


import com.ecg.comaas.mde.postprocessor.demandreporting.domain.Event;
import org.apache.http.client.HttpClient;

import java.util.concurrent.BlockingQueue;

public class HttpEventPublisherFactory implements EventPublisherFactory {

    private final BehaviorTrackingHandler.Config config;
    private HttpClient httpClient;

    public HttpEventPublisherFactory(BehaviorTrackingHandler.Config config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public Runnable create(BlockingQueue<Event> queue) {
        return new HttpEventPublisher(queue, config, httpClient);
    }
}
