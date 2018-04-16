package com.ecg.comaas.gtau.listener.messagelogger;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EventCreator {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String REPLY_CHANNEL_HEADER = "X-Reply-Channel";
    private static final String MAILER_CLIENT_INFO_HEADER = "X-Mailer";
    private static final String USER_AGENT = "User-Agent";
    private static final String HEADER_CATEGORY_ID = "categoryid";
    private static final String HEADER_IP = "ip";
    private static final String DEFAULT_REPLY_CHANNEL = "mail";
    private static final String EMPTY_VALUE = "-";

    private EventCreator() {
    }

    public static Map<String, String> toValues(Conversation conversation, Message message) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("messageId", message.getId());
        values.put("conversationId", conversation.getId());
        values.put("messageDirection", message.getMessageDirection().name());
        values.put("conversationState", conversation.getState().name());
        values.put("messageState", message.getState().name());
        values.put("adId", conversation.getAdId());
        values.put("sellerMail", conversation.getSellerId());
        values.put("buyerMail", conversation.getBuyerId());
        values.put("numOfMessageInConversation", Integer.toString(conversation.getMessages().indexOf(message)));
        values.put("logTimestamp", new DateTime().toString(DATE_FORMAT));
        values.put("conversationCreatedAt", conversation.getCreatedAt().toString(DATE_FORMAT));
        values.put("messageReceivedAt", message.getReceivedAt().toString(DATE_FORMAT));
        values.put("conversationLastModifiedDate", conversation.getLastModifiedAt().toString(DATE_FORMAT));
        values.put("custcategoryid", conversation.getCustomValues().getOrDefault(HEADER_CATEGORY_ID, EMPTY_VALUE));
        values.put("custip", conversation.getCustomValues().getOrDefault(HEADER_IP, EMPTY_VALUE));
        values.put("custuseragent", getCustomUserAgent(message));
        // non-existing means it is a mail-client -> mail-clients do not pass our X-REPLY-CHANNEL header
        values.put("custreplychannel", message.getHeaders().getOrDefault(REPLY_CHANNEL_HEADER, DEFAULT_REPLY_CHANNEL));
        return values;
    }

    private static String getCustomUserAgent(Message message) {
        if (message.getHeaders().containsKey(MAILER_CLIENT_INFO_HEADER)) {
            return message.getHeaders().get(MAILER_CLIENT_INFO_HEADER);
        } else if (message.getHeaders().containsKey(USER_AGENT)) {
            return message.getHeaders().get(USER_AGENT);
        }

        return EMPTY_VALUE;
    }
}
