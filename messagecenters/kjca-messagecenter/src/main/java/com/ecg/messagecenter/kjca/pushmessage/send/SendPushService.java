package com.ecg.messagecenter.kjca.pushmessage.send;

import com.ecg.messagecenter.kjca.pushmessage.PushMessagePayload;
import com.ecg.messagecenter.kjca.pushmessage.PushService;
import com.ecg.messagecenter.kjca.pushmessage.send.client.SendClient;
import com.ecg.messagecenter.kjca.pushmessage.send.client.SendException;
import com.ecg.messagecenter.kjca.pushmessage.send.model.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SendPushService extends PushService {

    private final static Logger LOG = LoggerFactory.getLogger(SendPushService.class);
    private final SendClient sendClient;

    private final PushMessageTransformer transformer;

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
}
