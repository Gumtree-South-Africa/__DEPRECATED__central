package com.ecg.messagecenter.kjca.pushmessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

public class ActiveMQPushServiceImpl extends PushService {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQPushServiceImpl.class);

    static final String QUEUE_NAME = "messageBoxMDNSQueue";
    static final String VERSION_HEADER = "Version";
    static final int NEW_PAYLOAD_VERSION = 2;

    private final JmsTemplate jmsTemplate;

    public ActiveMQPushServiceImpl(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public Result sendPushMessage(PushMessagePayload payload) {
        LOG.debug("Sending a push notification using ActiveMQ: '" + payload.getUserId() + "', '" + payload.getEmail() + "'");

        try {
            jmsTemplate.convertAndSend(QUEUE_NAME, payload.asJson(), message -> {
                message.setIntProperty(VERSION_HEADER, NEW_PAYLOAD_VERSION);
                return message;
            });
        } catch (JmsException e) {
            return Result.error(payload, e);
        }

        return Result.ok(payload);
    }
}
