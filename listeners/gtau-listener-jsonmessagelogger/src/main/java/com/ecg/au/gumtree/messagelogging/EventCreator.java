package com.ecg.au.gumtree.messagelogging;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.util.JsonObjects.Builder;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Determines the log event based on the given moderationState
 */
class EventCreator {
    private final static Logger LOG = LoggerFactory.getLogger(EventCreator.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final Pattern NON_PARSEABLE = Pattern.compile("[^a-zA-Z0-9]");

    public static final String REPLY_CHANNEL_HEADER = "X-Reply-Channel";
    public static final String MAILER_CLIENT_INFO_HEADER = "X-Mailer";
    public static final String USER_AGENT = "User-Agent";
    public static final List<String> INCLUDED_HEADERS = new ArrayList<String>();

    static {
        INCLUDED_HEADERS.add("categoryid");
        INCLUDED_HEADERS.add("ip");
    }

    public String jsonLogEntry(Conversation conversation, Message message) {
        MessageDirection messageDirection = message.getMessageDirection();

        Builder builder = JsonObjects
                .builder()
                .attr("messageId", message.getId())
                .attr("conversationId", conversation.getId())
                .attr("messageDirection", messageDirection.name())
                .attr("conversationState", conversation.getState().name())
                .attr("messageState", message.getState().name())
                .attr("adId", conversation.getAdId())
                .attr("sellerMail", conversation.getSellerId())
                .attr("buyerMail", conversation.getBuyerId())
                .attr("numOfMessageInConversation", messageIndex(conversation, message))
                .attr("logTimestamp", new DateTime().toString(DATE_FORMAT))
                .attr("conversationCreatedAt", conversation.getCreatedAt().toString(DATE_FORMAT))
                .attr("messageReceivedAt", message.getReceivedAt().toString(DATE_FORMAT))
                .attr("conversationLastModifiedDate", conversation.getLastModifiedAt().toString(DATE_FORMAT));

        includeConversationBasedCustomHeaders(conversation, builder);
        includeMessageBasedCustomHeaders(conversation, message, builder);

        return builder.toJson();

    }

    private int convertVersionToSemanticVersion(Message message) {
        // what comes in: version 0 = messageCreatedEvent, version 1 = messageFilteredEvent, version 2 = messageTerminatedEvent
        // what we want here: version 1 = message was filtered and sent/held/blocked, version 2 = message was moderated for the first time, version 3 = message was moderated for the second time
        return message.getVersion() - 1;
    }

    private void includeConversationBasedCustomHeaders(Conversation conversation, Builder builder) {
        Map<String, String> headers = Maps.filterKeys(conversation.getCustomValues(), Predicates.in(INCLUDED_HEADERS));
        for (Entry<String, String> customVal : headers.entrySet()) {
            LOG.trace(customVal.getKey());
            builder.attr(customValueKey(customVal.getKey()), customVal.getValue());
        }
    }

    private void includeMessageBasedCustomHeaders(Conversation conversation, Message message, Builder builder) {
        if (message.getHeaders().containsKey(MAILER_CLIENT_INFO_HEADER)) {
            builder.attr("custuseragent", message.getHeaders().get(MAILER_CLIENT_INFO_HEADER));
        } else {
            if (message.getHeaders().containsKey(USER_AGENT)) {
                builder.attr("custuseragent", message.getHeaders().get(USER_AGENT));
            }
        }

        if (message.getHeaders().containsKey(REPLY_CHANNEL_HEADER)) {
            builder.attr("custreplychannel", message.getHeaders().get(REPLY_CHANNEL_HEADER));
        } else {
            // nonexisting means it is a mail-client -> mail-clients do not pass our X-REPLY-CHANNEL header
            builder.attr("custreplychannel", "mail");
        }
    }

    private boolean replyMessage(Conversation conversation, Message message) {
        return messageIndex(conversation, message) > 0;
    }


    private String customValueKey(String key) {
        return "cust" + NON_PARSEABLE.matcher(key).replaceAll("").toLowerCase();
    }


    private int messageIndex(Conversation conversation, Message message) {
        return conversation.getMessages().indexOf(message);
    }
}
