package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.CORRELATION_ID;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.MAIL_FROM;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.TASK_NAME;

public class SharedBrain {

    private static final Logger LOG = LoggerFactory.getLogger(SharedBrain.class);

    private static final String HAZELCAST_TOPIC = "volumefilter_sender_exchange";
    private static final String TASK_VOLUME_NOTIFICATION_IN = "volume-notification-in";
    private static final String TASK_VOLUME_NOTIFICATION_OUT = "volume-notification-out";

    private final ITopic<MailReceivedNotification> communicationBus;
    private final EventStreamProcessor processor;

    SharedBrain(HazelcastInstance hazelcastInstance, EventStreamProcessor processor) {
        this.processor = processor;
        this.communicationBus = hazelcastInstance.getTopic(HAZELCAST_TOPIC);
        this.communicationBus.addMessageListener(this::processMessage);
    }

    private void processMessage(Message<MailReceivedNotification> message) {
        Member publishingMember = message.getPublishingMember();

        if (publishingMember == null || publishingMember.localMember()) {
            return;
        }

        MailReceivedNotification notification = message.getMessageObject();
        String mailAddress = notification.getMailAddress();
        String correlationId = notification.getCorrelationId();

        MDC.clear();
        MDC.put(TASK_NAME, TASK_VOLUME_NOTIFICATION_IN);
        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(MAIL_FROM, mailAddress);

        LOG.debug(TASK_VOLUME_NOTIFICATION_IN);

        processor.mailReceivedFrom(mailAddress);
        MDC.clear();
    }

    void markSeen(String mailAddress) {
        LOG.debug(TASK_VOLUME_NOTIFICATION_OUT);
        processor.mailReceivedFrom(mailAddress);
        communicationBus.publish(new MailReceivedNotification(mailAddress, MDC.get(CORRELATION_ID)));
    }
}
