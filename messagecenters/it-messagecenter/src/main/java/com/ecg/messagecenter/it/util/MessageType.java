package com.ecg.messagecenter.it.util;

import com.ecg.messagecenter.it.persistence.Header;
import com.ecg.replyts.core.api.model.conversation.Message;

/**
 * Created by mdarapour.
 */
public class MessageType {
    public static boolean isRobot(Message message) {
        return message.getCaseInsensitiveHeaders().containsKey(Header.Robot.getValue());
    }

    public static boolean isOffer(Message message) {
        return message.getCaseInsensitiveHeaders().containsKey(Header.OfferId.getValue());
    }

    public static String getRobot(Message message) {
        return message.getCaseInsensitiveHeaders().get(Header.Robot.getValue());
    }

    public static String getOffer(Message message) {
        return message.getCaseInsensitiveHeaders().get(Header.OfferId.getValue());
    }
}
