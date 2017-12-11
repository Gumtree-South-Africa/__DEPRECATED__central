package com.ecg.messagecenter.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.util.PushNotificationTextShortener;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class PushMessageOnUnreadConversationCallback implements SimplePostBoxInitializer.PostBoxWriteCallback {

    private static final Counter COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.push-message-sent");
    private static final Counter COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.push-message-no-device");
    private static final Counter COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.push-message-failed");

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);

    private final PushService pushService;
    private final AdImageLookup adImageLookup;
    private final Conversation conversation;
    private final Message message;

    public PushMessageOnUnreadConversationCallback(
            PushService pushService,
            AdImageLookup adImageLookup,
            Conversation conversation,
            Message message) {

        this.pushService = pushService;
        this.adImageLookup = adImageLookup;
        this.conversation = conversation;
        this.message = message;
    }

    @Override
    public void success(String email, Long unreadCount, boolean markedAsUnread) {
        // only push-trigger on unread use-case

        if (!markedAsUnread) {
            return;
        }

        sendPushMessage(email, unreadCount);
    }


    void sendPushMessage(String email, Long unreadCount) {
        try {
            Optional<PushMessagePayload> payload = createPayloadBasedOnNotificationRules(conversation, message, email, unreadCount);

            if (!payload.isPresent()) {
                return;
            }

            PushService.Result sendPushResult = pushService.sendPushMessage(payload.get());
            switch (sendPushResult.getStatus()) {
                case OK:
                    COUNTER_PUSH_SENT.inc();
                    break;
                case NOT_FOUND:
                    COUNTER_PUSH_NO_DEVICE.inc();
                    break;
                case ERROR:
                    COUNTER_PUSH_FAILED.inc();
                    LOG.warn(
                        "Error sending push for conversation '{}' and message '{}'",
                        conversation.getId(),
                        message.getId(),
                        sendPushResult.getException().get());
                    break;
            }
        } catch (Exception e) {
            COUNTER_PUSH_FAILED.inc();
            LOG.error("Error sending push for conversation '{}' and message '{}'", conversation.getId(), message.getId(), e);
        }
    }

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, String email, Long unreadCount) {
        HashMap<String, String> details = new HashMap<>();
        details.put("conversationId", conversation.getId());
        return Optional.of(
                new PushMessagePayload(
                        email,
                        createPushMessageText(conversation, message),
                        "CONVERSATION",
                        details,
                        Optional.of(unreadCount.intValue()),
                        Optional.of(gcmDetails(conversation, message)),
                        Optional.empty())
        );
    }

    private Map<String, String> gcmDetails(Conversation conversation, Message message) {
        return ImmutableMap.of(
                "senderInfo", senderInfo(message, conversation),
                "senderMessage", messageShort(message),
                "adImageUrl", adImageLookup.lookupAdImageUrl(Long.parseLong(conversation.getAdId())));
    }

    private String createPushMessageText(Conversation conversation, Message message) {
        return senderInfo(message, conversation) + ": " + messageShort(message);
    }

    private String senderInfo(Message message, Conversation conversation) {
        if (message.getMessageDirection() == BUYER_TO_SELLER) {
            String buyerName = conversation.getCustomValues().get("buyer-name");
            if (buyerName == null) {
                return "Interessent";
            } else {
                return buyerName;
            }
        }

        if (message.getMessageDirection() == SELLER_TO_BUYER) {
            return "Antwort vom Anbieter";
        }

        LOG.error("Undefined MessageDirection for message " + message.getId());
        return "";

    }

    private String messageShort(Message message) {
        String shortText = Strings.nullToEmpty(PushNotificationTextShortener.shortenText(message.getPlainTextBody()));

        if (message.getHeaders().containsKey("X-Offer-Value")) {
            String offer = "\nAngebot: "+message.getHeaders().get("X-Offer-Value");
            return (truncateText(shortText, 50 - offer.length()) + offer).trim();
        } else {
            return truncateText(shortText, 50);
        }

    }

    public static String truncateText(String description, int maxChars) {

        if (isNullOrEmpty(description))
            return "";

        if (description.length() <= maxChars) {
            return description;
        } else {
            String substring = description.substring(0, maxChars);
            return substringBeforeLast(substring, " ").concat("...");
        }
    }

    private static String substringBeforeLast(String str, String separator) {
        if (isNullOrEmpty(str) || isNullOrEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }


}
