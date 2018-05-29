package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.ecg.comaas.mde.postprocessor.demandreporting.domain.Event;

import java.util.concurrent.BlockingQueue;

public interface EventPublisherFactory {

    Runnable create(BlockingQueue<Event> queue);

}
