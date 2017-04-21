package com.ecg.de.ebayk.messagecenter.util;

import com.ecg.de.ebayk.messagecenter.persistence.Header;
import com.ecg.replyts.core.api.model.conversation.Message;

/**
 * Created by mdarapour.
 */
public class MessageType {
    public static boolean isRobot(Message message) {
        return message.getHeaders().containsKey(Header.Robot.getValue());
    }

    public static boolean isOffer(Message message) {
        return message.getHeaders().containsKey(Header.OfferId.getValue());
    }

    public static String getRobot(Message message) {
        return message.getHeaders().get(Header.Robot.getValue());
    }

    public static String getOffer(Message message) {
        return message.getHeaders().get(Header.OfferId.getValue());
    }
}
