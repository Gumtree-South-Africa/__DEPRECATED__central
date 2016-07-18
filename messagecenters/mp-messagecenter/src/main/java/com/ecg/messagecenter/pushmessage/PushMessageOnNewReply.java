package com.ecg.messagecenter.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.persistence.NewMessageListener;
import com.ecg.messagecenter.util.PushNotificationTextShortener;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static java.lang.String.format;

public class PushMessageOnNewReply implements NewMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushMessageOnNewReply.class);

    private static final Counter COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.push-message-sent");
    private static final Counter COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.push-message-no-device");
    private static final Counter COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.push-message-failed");

    private final PushService pushService;
    private final AdImageLookup adImageLookup;
    private final Conversation conversation;
    private final Message message;

    public PushMessageOnNewReply(
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
    public void success(String recipientUserId, long unreadCount, boolean isNewReply) {
        // only send push notifications in case of new replies
        if (!isNewReply) {
            return;
        }

        sendPushMessage(recipientUserId, unreadCount);
    }

    void sendPushMessage(String recipientUserId, long unreadCount) {
        try {

            Optional<PushMessagePayload> payload = createPayloadBasedOnNotificationRules(conversation, message, recipientUserId, unreadCount);

            if (!payload.isPresent()) {
                return;
            }

            PushService.Result sendPushResult = pushService.sendPushMessage(payload.get());

            if (PushService.Result.Status.OK == sendPushResult.getStatus()) {
                COUNTER_PUSH_SENT.inc();
            }

            if (PushService.Result.Status.NOT_FOUND == sendPushResult.getStatus()) {
                COUNTER_PUSH_NO_DEVICE.inc();
            }

            if (PushService.Result.Status.ERROR == sendPushResult.getStatus()) {
                COUNTER_PUSH_FAILED.inc();
                LOGGER.error(format("Error sending push for conversation '%s' and message '%s'", conversation.getId(), message.getId()), sendPushResult.getException().get());
            }

        } catch (Exception e) {
            COUNTER_PUSH_FAILED.inc();
            LOGGER.error(format("Error sending push for conversation '%s' and message '%s'", conversation.getId(), message.getId()), e);
        }
    }

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, String recipientUserId, long unreadCount) {
        return Optional.of(
                new PushMessagePayload(
                        recipientUserId,
                        createPushMessageText(conversation, message),
                        "CONVERSATION",
                        ImmutableMap.of("conversationId", conversation.getId()),
                        Optional.of(unreadCount),
                        Optional.of(gcmDetails(conversation, message)),
                        Optional.<Map<String, String>>empty())
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

        LOGGER.error("Undefined MessageDirection for message " + message.getId());
        return "";
    }

    private String messageShort(Message message) {
        String shortText = Strings.nullToEmpty(PushNotificationTextShortener.shortenText(message.getPlainTextBody()));

        if (message.getHeaders().containsKey("X-Offer-Value")) {
            String offer = "\nAngebot: " + message.getHeaders().get("X-Offer-Value");
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

    private static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }
}
