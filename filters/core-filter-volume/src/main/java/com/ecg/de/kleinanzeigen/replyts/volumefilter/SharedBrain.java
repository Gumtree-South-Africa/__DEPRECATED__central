package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.core.*;

public class SharedBrain {

    private static final String VOLUMEFILTER_SENDER_ADDRESS_EXCHANGE = "volumefilter_sender_address_exchange";

    private final ITopic<String> communicationBus;
    private final EventStreamProcessor processor;

    SharedBrain(HazelcastInstance hazelcastInstance, EventStreamProcessor processor) {
        this.processor = processor;
        this.communicationBus = hazelcastInstance.getReliableTopic(VOLUMEFILTER_SENDER_ADDRESS_EXCHANGE);
        this.communicationBus.addMessageListener(message -> {
            Member publishingMember = message.getPublishingMember();
            if (publishingMember != null && !publishingMember.localMember()) {
                processor.mailReceivedFrom(message.getMessageObject());
            }
        });
    }

    void markSeen(String mailAddress) {
        processor.mailReceivedFrom(mailAddress);
        communicationBus.publish(mailAddress);
    }
}
