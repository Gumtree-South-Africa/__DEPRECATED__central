package com.ecg.comaas.gtau.filter.volumefilter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

import java.util.concurrent.atomic.AtomicReference;

class SharedBrain {
    private final ITopic<String> communicationBus;
    private final AtomicReference<EventStreamProcessor> processor = new AtomicReference<EventStreamProcessor>();

    SharedBrain(HazelcastInstance hazelcastInstance) {
        communicationBus = hazelcastInstance.getTopic("volumefilter_sender_address_exchange");
        communicationBus.addMessageListener(message -> processor.get().mailReceivedFrom(message.getMessageObject()));
    }

    public void withProcessor(EventStreamProcessor processor) {
        this.processor.set(processor);
    }

    public void markSeen(String mailAddress) {
        communicationBus.publish(mailAddress);
    }
}
