package com.ecg.messagecenter.bt.pushmessage;

import static java.lang.String.format;

import java.util.Map;

import com.ecg.messagecenter.bt.persistence.PostBoxInitializer;
import com.ecg.messagecenter.bt.util.MessageTextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Optional;

public class PushMessageOnUnreadConversationCallback implements PostBoxInitializer.PostBoxWriteCallback {
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

            if (payload == null || !payload.isPresent()) {
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
                LOG.error(format("Error sending push for conversation '%s' and message '%s'", conversation.getId(), message.getId()), sendPushResult.getException().get());
            }
        } catch (Exception e) {
            COUNTER_PUSH_FAILED.inc();

            LOG.error(format("Error sending push for conversation '%s' and message '%s'", conversation.getId(), message.getId()), e);
        }
    }

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, String email, Long unreadCount) {
    	String receiverId = getReceiverId(conversation, message.getMessageDirection());

        if (!StringUtils.hasText(receiverId)) {
        	LOG.warn("Message don't have the direction/receiverId can't be set no push sent out, could be non-registed user. that's ok. for message: " + message.getId());

        	return null;
        }

        Map<String,String> pushMap = new HashMap<>();

    	pushMap.put("conversationId", conversation.getId());
    	pushMap.put("receiverId", receiverId);
    	pushMap.put("senderId", getSenderId(conversation, message.getMessageDirection()));
    	pushMap.put("adId", conversation.getAdId());
    	pushMap.put("adTitle", conversation.getCustomValues().get("mc-adtitle"));
    	pushMap.put("badge", String.valueOf(unreadCount));

    	if (conversation.getCustomValues().get("locale") != null) {
    		pushMap.put("locale", conversation.getCustomValues().get("locale"));
    	}

    	if (conversation.getCustomValues().get("mc-adthumbnail") != null) {
            pushMap.put("adImage", conversation.getCustomValues().get("mc-adthumbnail"));                
    	}

        return Optional.of(new PushMessagePayload(
          email,
          createPushMessageText(conversation, message),
          "CHATMESSAGE",
          pushMap,
          Optional.of(unreadCount != null ? unreadCount.intValue() : null),
          Optional.of(gcmDetails(conversation, message)),
          Optional.empty()));
    }
    
    private static String getReceiverId(Conversation conversation, MessageDirection direction) {
    	if (direction != MessageDirection.BUYER_TO_SELLER && direction != MessageDirection.SELLER_TO_BUYER) {
    		return null;
    	}

    	String receiverId = conversation.getCustomValues().get(direction == MessageDirection.BUYER_TO_SELLER ? "mc-sellerid" : "mc-buyerid");

    	if (!StringUtils.hasText(receiverId)) {
    		receiverId = BoltUtil.getUserId(direction == MessageDirection.BUYER_TO_SELLER ? conversation.getSellerId() : conversation.getBuyerId());
    	}

    	LOG.info(String.format("push message direction %s: recevierId=%s", direction == MessageDirection.BUYER_TO_SELLER ? "buyer to seller" : "seller to buyer", receiverId));

    	return receiverId;
    }

    
    private static String getSenderId(Conversation conversation, MessageDirection direction) {
    	if (direction != MessageDirection.BUYER_TO_SELLER && direction != MessageDirection.SELLER_TO_BUYER) {
    		return null;
    	}

    	String senderId = conversation.getCustomValues().get(direction == MessageDirection.BUYER_TO_SELLER ? "mc-buyerid" : "mc-sellerid");

    	if (!StringUtils.hasText(senderId)) {
    		senderId = BoltUtil.getUserId(direction == MessageDirection.BUYER_TO_SELLER ? conversation.getBuyerId() : conversation.getSellerId());
    	}

    	LOG.info(String.format("Push message direction %s: senderId=%s", direction == MessageDirection.BUYER_TO_SELLER ? "buyer to seller" : "seller to buyer", senderId));

    	return senderId;
    }

    private Map<String, String> gcmDetails(Conversation conversation, Message message) {
        return ImmutableMap.of(
          "senderInfo", senderInfo(message, conversation),
          "senderMessage", messageShort(message),
          "adImageUrl", adImageLookup.lookupAdImageUrl(Long.parseLong(conversation.getAdId())));
    }

    private String createPushMessageText(Conversation conversation, Message message) {
        return senderInfo(message,conversation) + ": " + messageShort(message);
    }

    private String senderInfo(Message message, Conversation conversation) {
        switch(message.getMessageDirection()) {
            case BUYER_TO_SELLER:
                String buyerName = conversation.getCustomValues().get("buyer-name");

                if (pushService instanceof InfoPoolPushService) {
                    return conversation.getBuyerId() != null ? conversation.getBuyerId() : "dummy@ebay.com";
                }

                return buyerName != null ? buyerName : "Gumtree User";
            case SELLER_TO_BUYER:
                String sellerName = conversation.getCustomValues().get("seller-name");

                return sellerName != null ? sellerName : "Gumtree User";
            default:
                LOG.error("Undefined MessageDirection for message {}", message.getId());

                return "Gumtree User";
        }
    }

    private String messageShort(Message message) {
        String shortText = MessageTextHandler.remove(message.getPlainTextBody());

        if (shortText != null) {
            return truncateText(shortText, 50);
        }

        return "";
    }

    public static String truncateText(String description, int maxChars) {
        if (!StringUtils.hasText(description)) {
            return "";
        } else if (description.length() <= maxChars) {
            return description;
        } else {
            String substring = description.substring(0, maxChars);

            return substringBeforeLast(substring, " ").concat("...");
        }
    }

    private static String substringBeforeLast(String str, String separator) {
        if (!StringUtils.hasText(str) || !StringUtils.hasText(separator)) {
            return str;
        }

        int pos = str.lastIndexOf(separator);

        return pos == -1 ? str : str.substring(0, pos);
    }
}