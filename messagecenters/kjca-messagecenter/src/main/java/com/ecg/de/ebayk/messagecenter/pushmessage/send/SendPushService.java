package com.ecg.de.ebayk.messagecenter.pushmessage.send;

import com.codahale.metrics.Counter;
import com.ecg.de.ebayk.messagecenter.pushmessage.PushMessagePayload;
import com.ecg.de.ebayk.messagecenter.pushmessage.PushService;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.client.SendClient;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.client.SendException;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.model.SendMessage;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SendPushService extends PushService {

    private final static Logger LOG = LoggerFactory.getLogger(SendPushService.class);
    private final SendClient sendClient;

    private final PushMessageTransformer transformer;

    private static final Counter SEND_COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.send.push-message-sent");
    private static final Counter SEND_COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.send.push-message-no-device");
    private static final Counter SEND_COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.send.push-message-failed");

    @Autowired
    public SendPushService(SendClient sendClient) {
        this.sendClient = sendClient;
        this.transformer = new PushMessageTransformer();
    }

    @Override
    public Result sendPushMessage(PushMessagePayload payload) {
        SendMessage messageRequest = transformer.from(payload);
        try {
            if (sendClient.hasSubscription(messageRequest)) {
                SendMessage messageResponse = sendClient.sendMessage(messageRequest);
                LOG.debug("Sent message successfully, message Id is [{}]", messageResponse.getId());
                return Result.ok(payload);
            }
            LOG.debug("Message dropped due to no subscription");
            return Result.notFound(payload);
        } catch (SendException e) {
            LOG.warn("Not able to send message through SEND - internal cause {}", e.getInternalCause());
            return Result.error(payload, e);
        }
    }

    @Override
    protected Counter getPushFailedCounter() {
        return SEND_COUNTER_PUSH_FAILED;
    }

    @Override
    protected Counter getPushNoDeviceCounter() {
        return SEND_COUNTER_PUSH_NO_DEVICE;
    }

    @Override
    protected Counter getPushSentCounter() {
        return SEND_COUNTER_PUSH_SENT;
    }
}
