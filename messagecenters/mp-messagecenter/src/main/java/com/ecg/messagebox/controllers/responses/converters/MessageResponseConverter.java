package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.model.Message;
import org.springframework.stereotype.Component;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;

@Component
public class MessageResponseConverter {

    public MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId().toString(),
                message.getType().getValue(),
                message.getText(),
                message.getSenderUserId(),
                toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedDate()),
                message.getCustomData()
        );
    }
}