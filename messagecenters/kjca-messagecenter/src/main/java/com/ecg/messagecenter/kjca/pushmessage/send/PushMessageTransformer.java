package com.ecg.messagecenter.kjca.pushmessage.send;

import com.ecg.messagecenter.kjca.pushmessage.PushMessagePayload;
import com.ecg.messagecenter.kjca.pushmessage.send.client.SendClient;
import com.ecg.messagecenter.kjca.pushmessage.send.model.SendMessage;

import java.util.Map;

public class PushMessageTransformer {

    public SendMessage from(PushMessagePayload pushMessagePayload) {
        Map<String, String> details = pushMessagePayload.getDetails().orElse(null);

        return new SendMessage.MessageBodyBuilder()
                .setUserId(Long.valueOf(pushMessagePayload.getUserId()))
                .setType(SendClient.NotificationType.CHATMESSAGE)
                .setReferenceId(details != null ? details.get("ConversationId") : "")
                .setAlertCounter(pushMessagePayload.getAlertCounter().orElse(null))
                .setMessage(pushMessagePayload.getMessage())
                .setDetails(details)
                .createMessageBody();
    }
}
