package com.ecg.au.gumtree.messagelogging;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventCreator {
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static final String REPLY_CHANNEL_HEADER = "X-Reply-Channel";
    public static final String MAILER_CLIENT_INFO_HEADER = "X-Mailer";
    public static final String USER_AGENT = "User-Agent";

    private static final String HEADER_CATEGORY_ID = "categoryid";
    private static final String HEADER_IP = "ip";

    public static final String DEFAULT_REPLY_CHANNEL = "mail";

    public static final String EMPTY_VALUE = "-";

    public static Map<String, String> toValues(Conversation conversation, Message message) {
        return new LinkedHashMap<String, String>() {{
          put("messageId", message.getId());
          put("conversationId", conversation.getId());
          put("messageDirection", message.getMessageDirection().name());
          put("conversationState", conversation.getState().name());
          put("messageState", message.getState().name());
          put("adId", conversation.getAdId());
          put("sellerMail", conversation.getSellerId());
          put("buyerMail", conversation.getBuyerId());
          put("numOfMessageInConversation", Integer.toString(conversation.getMessages().indexOf(message)));
          put("logTimestamp", new DateTime().toString(DATE_FORMAT));
          put("conversationCreatedAt", conversation.getCreatedAt().toString(DATE_FORMAT));
          put("messageReceivedAt", message.getReceivedAt().toString(DATE_FORMAT));
          put("conversationLastModifiedDate", conversation.getLastModifiedAt().toString(DATE_FORMAT));
          put("custcategoryid", conversation.getCustomValues().getOrDefault(HEADER_CATEGORY_ID, EMPTY_VALUE));
          put("custip", conversation.getCustomValues().getOrDefault(HEADER_IP, EMPTY_VALUE));
          put("custuseragent", getCustomUserAgent(message));
          put("custreplychannel", getCustomReplyChannel(message));
        }};
    }

    private static String getCustomUserAgent(Message message) {
        if (message.getHeaders().containsKey(MAILER_CLIENT_INFO_HEADER)) {
            return message.getHeaders().get(MAILER_CLIENT_INFO_HEADER);
        } else if (message.getHeaders().containsKey(USER_AGENT)) {
            return message.getHeaders().get(USER_AGENT);
        }

        return EMPTY_VALUE;
    }

    private static String getCustomReplyChannel(Message message) {
        return message.getHeaders().getOrDefault(REPLY_CHANNEL_HEADER, DEFAULT_REPLY_CHANNEL); // non-existing means it is a mail-client -> mail-clients do not pass our X-REPLY-CHANNEL header
    }
}
