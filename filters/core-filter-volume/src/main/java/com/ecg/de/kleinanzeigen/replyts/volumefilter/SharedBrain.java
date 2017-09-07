package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.marker.Markers.append;

public class SharedBrain {

    private static final Logger LOG = LoggerFactory.getLogger(SharedBrain.class);

    private static final String VOLUMEFILTER_SENDER_ADDRESS_EXCHANGE = "volumefilter_sender_address_exchange";

    private final ITopic<String> communicationBus;
    private final EventStreamProcessor processor;

    SharedBrain(HazelcastInstance hazelcastInstance, EventStreamProcessor processor) {
        this.processor = processor;
        this.communicationBus = hazelcastInstance.getTopic(VOLUMEFILTER_SENDER_ADDRESS_EXCHANGE);
        this.communicationBus.addMessageListener(message -> {
            Member publishingMember = message.getPublishingMember();
            if (publishingMember != null && !publishingMember.localMember()) {
                LOG.debug(append("notification_value", message.getMessageObject()), "volume-notification-in");
                processor.mailReceivedFrom(message.getMessageObject());
            }
        });
    }

    void markSeen(String mailAddress) {
        LOG.trace(append("notification_value", mailAddress), "volume-notification-out");
        processor.mailReceivedFrom(mailAddress);
        communicationBus.publish(mailAddress);
    }
}
