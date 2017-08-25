package com.ecg.messagecenter.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.gumtree.replyts2.common.message.MessageTextHandler;
import com.ecg.messagecenter.persistence.SimplePostBoxInitializer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class PushMessageOnUnreadConversationCallback implements SimplePostBoxInitializer.PostBoxWriteCallback {
    private static final Logger LOG = LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);

    private static final Counter COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.push-message-sent");
    private static final Counter COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.push-message-no-device");
    private static final Counter COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.push-message-failed");

    private static final int MAX_CHARS = 50;
    private static final String SEPARATOR = " ";

    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final Conversation conversation;
    private final Message message;

    public PushMessageOnUnreadConversationCallback(
            PushService pushService,
            AdInfoLookup adInfoLookup,
            Conversation conversation,
            Message message) {
        this.pushService = checkNotNull(pushService);
        this.adInfoLookup = checkNotNull(adInfoLookup);
        this.conversation = conversation;
        this.message = message;
    }

    @Override
    public void success(String email, Long unreadCount, boolean markedAsUnread) {
        // only push-trigger on unread use-case
        if (markedAsUnread) {
            sendPushMessage(email, unreadCount);
        }
    }

    void sendPushMessage(String email, Long unreadCount) {
        try {
            final AdInfoLookup.AdInfo adInfo = adInfoLookup.lookupAdIInfo(Long.parseLong(conversation.getAdId())).orElse(null);

            createPayloadBasedOnNotificationRules(conversation, message, email, unreadCount, adInfo)
                    .ifPresent(this::sendPushMessage);

        } catch (Exception e) {
            COUNTER_PUSH_FAILED.inc();
            LOG.error("Error sending push for conversation '{}' and message '{}'", conversation.getId(), message.getId(), e);
        }

    }

    private void sendPushMessage(PushMessagePayload payload) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending push message, Payload:[{}]", payload.asJson());
        }
        incCounters(pushService.sendPushMessage(payload));
    }

    private void incCounters(PushService.Result sendPushResult) {
        switch (sendPushResult.getStatus()) {
            case OK:
                COUNTER_PUSH_SENT.inc();
                break;

            case NOT_FOUND:
                COUNTER_PUSH_NO_DEVICE.inc();
                break;

            case ERROR:
                COUNTER_PUSH_FAILED.inc();
                LOG.error("Error sending push for conversation '{}' and message '{}'",
                        conversation.getId(), message.getId(), sendPushResult.getException());
                break;
        }
    }

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, String email, Long unreadCount, AdInfoLookup.AdInfo adInfo) {
        String pushMessage = createPushMessageText(conversation, message);
        return Optional.of(
                new PushMessagePayload(
                        email,
                        pushMessage,
                        "CHATMESSAGE",
                        ImmutableMap.of("ConversationId", conversation.getId()),
                        Optional.of(unreadCount.intValue()),
                        Optional.of(gcmDetails(conversation, message, adInfo)),
                        Optional.of(apnsDetails(conversation, message, adInfo))));
    }

    private Map<String, String> gcmDetails(final Conversation conversation, Message message, AdInfoLookup.AdInfo adInfo) {
        String messageShort = messageShort(message);
        return ImmutableMap.<String, String>builder()
                .put("senderInfo", senderInfo(message, conversation))
                .put("senderMessage", messageShort)
                .put("AdImageUrl", adInfo == null ? "" : adInfo.getImageUrl())
                .put("title", messageShort)
                .put("type", "3")
                .put("AdId", conversation.getAdId())
                .put("AdSubject", adInfo == null ? "" : adInfo.getTitle())
                .build();
    }

    private Map<String, String> apnsDetails(final Conversation conversation, Message message, AdInfoLookup.AdInfo adInfo) {
        String messageBody = messageShort(message);
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("senderInfo", senderInfo(message, conversation))
                .put("senderMessage", messageBody)
                .put("adImageUrl", adInfo == null ? "" : adInfo.getImageUrl())
                .put("title", messageBody);

        JSONObject ebay = new JSONObject();
        ebay.put("AdImageUrl", adInfo == null ? "" : adInfo.getImageUrl());
        ebay.put("AdSubject", adInfo == null ? "" : adInfo.getTitle());
        ebay.put("Type", "3");
        ebay.put("ConversationId", conversation.getId());
        ebay.put("AdId", conversation.getAdId());
        builder.put("ebay", ebay.toString());

        return builder.build();
    }

    private String createPushMessageText(Conversation conversation, Message message) {
        return MessageTextHandler.remove(senderInfo(message, conversation)) + ": " + messageShort(message);
    }

    private String senderInfo(Message message, Conversation conversation) {
        if (message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
            String buyerName = conversation.getCustomValues().get("buyer-name");
            if (buyerName == null) {
                return "Buyer";
            } else {
                return buyerName;
            }
        }

        if (message.getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
            return "Response from the seller";
        }

        LOG.error("Undefined MessageDirection for message " + message.getId());
        return "";

    }

    private String messageShort(Message message) {
        String shortText = MessageTextHandler.remove(message.getPlainTextBody());

        if (shortText != null) {
            return truncateText(shortText);
        }
        return "";

    }

    private static String truncateText(String description) {
        if (isNullOrEmpty(description)) {
            return "";
        }
        if (description.length() <= MAX_CHARS) {
            return description;
        } else {
            String substring = description.substring(0, MAX_CHARS);
            return substringBeforeLast(substring).concat("...");
        }
    }

    private static String substringBeforeLast(String str) {
        if (isNullOrEmpty(str) || isNullOrEmpty(SEPARATOR)) {
            return str;
        }
        int pos = str.lastIndexOf(SEPARATOR);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    private static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }
}