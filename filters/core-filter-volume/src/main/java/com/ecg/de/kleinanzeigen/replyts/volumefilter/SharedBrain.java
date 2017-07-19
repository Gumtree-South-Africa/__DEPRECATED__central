package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.core.*;

public class SharedBrain {

    private final ITopic<String> communicationBus;
    private final EventStreamProcessor processor;

    SharedBrain(HazelcastInstance hazelcastInstance, EventStreamProcessor processor) {
        this.processor = processor;
        this.communicationBus = hazelcastInstance.getTopic("volumefilter_sender_address_exchange");
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
