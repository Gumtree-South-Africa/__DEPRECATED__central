package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

public class ActiveMQPushServiceImpl extends PushService {

    static final String QUEUE_NAME = "messageBoxMDNSQueue";
    static final String VERSION_HEADER = "Version";
    static final int NEW_PAYLOAD_VERSION = 2;

    private static final Counter JMS_EXCEPTION_COUNTER = TimingReports.newCounter("message-box.jms.exceptions");

    private final JmsTemplate jmsTemplate;

    public ActiveMQPushServiceImpl(
            JmsTemplate jmsTemplate
    ) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public Result sendPushMessage(PushMessagePayload payload) {
        try {
            jmsTemplate.convertAndSend(QUEUE_NAME, payload.asJson(), message -> {
                message.setIntProperty(VERSION_HEADER, NEW_PAYLOAD_VERSION);
                return message;
            });
        } catch (JmsException e) {
            JMS_EXCEPTION_COUNTER.inc();
            return Result.error(payload, e);
        }

        return Result.ok(payload);
    }

}
