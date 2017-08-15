package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class SharedBrain {

    private final ITopic<String> communicationBus;
    private final AtomicReference<EventStreamProcessor> processor = new AtomicReference<EventStreamProcessor>();

    SharedBrain(HazelcastInstance hazelcastInstance) {
        communicationBus = hazelcastInstance.getReliableTopic("volumefilter_sender_address_exchange");
        communicationBus.addMessageListener(new MessageListener<String>() {
            @Override
            public void onMessage(Message<String> message) {
                processor.get().mailReceivedFrom(message.getMessageObject());
            }
        });
    }

    public void withProcessor(EventStreamProcessor processor) {
        this.processor.set(processor);
    }


    public void markSeen(String mailAddress) {
        communicationBus.publish(mailAddress);
    }

}
