package com.ecg.comaas.gtuk.filter.volume;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class SharedBrain {
    private static final Logger LOG = LoggerFactory.getLogger(SharedBrain.class);
    static final String GUMTREE_VELOCITY_FILTER_EXCHANGE = "gumtree_velocity_filter_exchange";
    private final ITopic<String> communicationBus;
    private final AtomicReference<EventStreamProcessor> processor = new AtomicReference<>();
    private static AtomicReference<String> messageListenerId = new AtomicReference<>();

    SharedBrain(HazelcastInstance hazelcastInstance, EventStreamProcessor eventStreamProcessor) {
        this.processor.set(eventStreamProcessor);
        communicationBus = hazelcastInstance.getTopic(GUMTREE_VELOCITY_FILTER_EXCHANGE);
        updateMessageListener();
    }

    private void updateMessageListener() {
        String newMessageListenerId = communicationBus.addMessageListener(message -> processor.get().mailReceivedFrom(message.getMessageObject()));
        LOG.info("Registered new message listener {}", newMessageListenerId);

        String oldMessageId = messageListenerId.getAndSet(newMessageListenerId);
        if (oldMessageId != null) {
            boolean removalResult = communicationBus.removeMessageListener(oldMessageId);
            LOG.info("Removed old message listener [{}] result: {}", oldMessageId, removalResult);
        }
    }

    void markSeen(String emailAddress, String ipAddress, String cookieId) {
        if (emailAddress != null) {
            LOG.trace("Publishing email address [{}] to all known rts nodes", emailAddress);
            communicationBus.publish(emailAddress);
        }
        if (ipAddress != null) {
            LOG.trace("Publishing ip address [{}] to all known rts nodes", ipAddress);
            communicationBus.publish(ipAddress);
        }
        if (cookieId != null) {
            LOG.trace("Publishing cookie id [{}] to all known rts nodes", cookieId);
            communicationBus.publish(cookieId);
        }
    }
}
