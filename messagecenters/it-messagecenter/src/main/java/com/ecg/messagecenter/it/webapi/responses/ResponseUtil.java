package com.ecg.messagecenter.it.webapi.responses;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by jaludden on 26/01/16.
 */
public class ResponseUtil {

    private static final String DEFAULT_USER_NAME = "Utente di Kijiji";
    private static final String DEFAULT_MAIL_USER = "Mail Delivery System";
    private static final Pattern EMAIL_PATTERN =
                    Pattern.compile("[a-zA-Z0-9._-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");

    private static final Logger LOG = LoggerFactory.getLogger(ResponseUtil.class);

    public static boolean hasName(String buyerName) {
        if (StringUtil.isBlank(buyerName)) {
            return false;
        }
        if (buyerName.equals(DEFAULT_USER_NAME)) {
            return false;
        }
        if (buyerName.equals(DEFAULT_MAIL_USER)) {
            return false;
        }
        if (EMAIL_PATTERN.matcher(buyerName).find()) {
            return false;
        }
        return true;
    }

    public static String getName(List<Message> messages, MessageDirection direction) {
        for (Message message : messages) {
            try {
                String field = direction == message.getMessageDirection() ? "From" : "To";
                Map<String, String> headers = message.getCaseInsensitiveHeaders();
                if (headers != null && headers.get(field) != null) {
                    String name = new InternetAddress(message.getCaseInsensitiveHeaders().get(field))
                                    .getPersonal();
                    if (hasName(name)) {
                        return name;
                    }
                }
            } catch (AddressException e) {
            }
        }
        return DEFAULT_USER_NAME;
    }

    public static String getSecureName(String name) {
        if (hasName(name)) {
            return name;
        }
        return "";
    }
}
