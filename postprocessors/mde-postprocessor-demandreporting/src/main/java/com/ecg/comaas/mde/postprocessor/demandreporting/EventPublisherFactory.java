package com.ecg.comaas.mde.postprocessor.demandreporting;

import de.mobile.analytics.domain.Event;

import java.util.concurrent.BlockingQueue;

public interface EventPublisherFactory {
    Runnable create(BlockingQueue<Event> queue);
}
