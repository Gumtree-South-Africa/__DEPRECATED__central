package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.CORRELATION_ID;
import static net.logstash.logback.marker.Markers.append;

public class SharedBrain {

    private static final Logger LOG = LoggerFactory.getLogger(SharedBrain.class);

    private static final String VOLUMEFILTER_SENDER_EXCHANGE = "volumefilter_sender_exchange";
    private static final String TASK_NAME = "volume-notification-in";

    private final ITopic<MailReceivedNotification> communicationBus;
    private final EventStreamProcessor processor;

    SharedBrain(HazelcastInstance hazelcastInstance, EventStreamProcessor processor) {
        this.processor = processor;
        this.communicationBus = hazelcastInstance.getTopic(VOLUMEFILTER_SENDER_EXCHANGE);
        this.communicationBus.addMessageListener(message -> {
            Member publishingMember = message.getPublishingMember();
            if (publishingMember != null && !publishingMember.localMember()) {
                MailReceivedNotification notification = message.getMessageObject();
                MDCConstants.setTaskFields(TASK_NAME, notification.getCorrelationId());
                LOG.info(append("notification_value", notification.getMailAddress()), TASK_NAME);
                processor.mailReceivedFrom(notification.getMailAddress());
                MDC.clear();
            }
        });
    }

    void markSeen(String mailAddress) {
        LOG.trace(append("notification_value", mailAddress), "volume-notification-out");
        processor.mailReceivedFrom(mailAddress);
        communicationBus.publish(new MailReceivedNotification(mailAddress, MDC.get(CORRELATION_ID)));
    }
}
