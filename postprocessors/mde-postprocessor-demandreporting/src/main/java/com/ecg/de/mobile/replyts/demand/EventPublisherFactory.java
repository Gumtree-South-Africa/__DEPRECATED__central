package com.ecg.de.mobile.replyts.demand;

import de.mobile.analytics.domain.Event;

import java.util.concurrent.BlockingQueue;

public interface EventPublisherFactory {
    Runnable create(BlockingQueue<Event> queue);
}
