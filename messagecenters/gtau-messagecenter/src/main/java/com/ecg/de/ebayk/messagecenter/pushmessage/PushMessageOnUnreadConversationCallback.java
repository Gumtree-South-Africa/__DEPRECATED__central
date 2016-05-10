package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.de.ebayk.messagecenter.cleanup.TextCleaner;
import com.ecg.de.ebayk.messagecenter.persistence.Header;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxInitializer;
import com.ecg.de.ebayk.messagecenter.util.MessageContentHelper;
import com.ecg.de.ebayk.messagecenter.util.MessageType;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class PushMessageOnUnreadConversationCallback implements PostBoxInitializer.PostBoxWriteCallback {

    private static final Counter COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.push-message-sent");
    private static final Counter COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.push-message-no-device");
    private static final Counter COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.push-message-failed");

    private static final String GUMTREE_SENDER = "The Gumtree Team";

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);

    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final Conversation conversation;
    private final Message message;

    public PushMessageOnUnreadConversationCallback(
            PushService pushService,
            AdInfoLookup adInfoLookup,
            Conversation conversation,
            Message message) {

        this.pushService = pushService;
        this.adInfoLookup = adInfoLookup;
        this.conversation = conversation;
        this.message = message;
    }

    @Override
    public void success(PostBox postBox, boolean markedAsUnread) {

        // only push-trigger on unread use-case
        if (!markedAsUnread) {
            return;
        }

        sendPushMessage(postBox);
    }


    void sendPushMessage(PostBox postBox) {
        try {
            Optional<AdInfoLookup.AdInfo> adInfo = adInfoLookup.lookupAdIInfo(Long.parseLong(conversation.getAdId()));

            Optional<PushMessagePayload> payload = createPayloadBasedOnNotificationRules(conversation, message, postBox, adInfo);

            if (!payload.isPresent()) {
                return;
            }

            // No push notification for robot messages until the app is ready
            //if (MessageType.isRobot(message)) {
            //    return;
            //}

            LOG.debug("Sending push message, Payload:["+payload.get().asJson()+"]");
            PushService.Result sendPushResult = PushService.Result.ok(null);
            if (pushService != null) {
                sendPushResult = pushService.sendPushMessage(payload.get());
            }

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


    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, PostBox postBox, Optional<AdInfoLookup.AdInfo> adInfo) {
        String pushMessage = createPushMessageText(conversation, message);
        return Optional.of(
                new PushMessagePayload(
                        postBox.getEmail(),
                        pushMessage,
                        "CHATMESSAGE",
                        ImmutableMap.of("ConversationId", conversation.getId()),
                        Optional.of(postBox.getUnreadConversations().size()),
                        Optional.of(gcmDetails(conversation, message, adInfo, pushMessage, MessageType.isRobot(message))),
                        Optional.of(apnsDetails(conversation,message, adInfo, MessageType.isRobot(message))))
        );
    }

    private Map<String, String> gcmDetails(final Conversation conversation, Message message, Optional<AdInfoLookup.AdInfo> adInfo, String pushMessage, Boolean isRobot) {
        String messageBody = messageShort(message);
        return ImmutableMap.<String,String>builder()
                .put("senderInfo", senderInfo(message, conversation))
                .put("senderMessage", messageBody)
                .put("AdImageUrl", adInfo.transform(new Function<AdInfoLookup.AdInfo, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable AdInfoLookup.AdInfo input) {
                        return Optional.fromNullable(input.getImageUrl()).or("");
                    }
                }).or(""))
                .put("title", pushMessage)
                .put("type","3")
                .put("AdId",conversation.getAdId())
                .put("AdSubject", adInfo.transform(new Function<AdInfoLookup.AdInfo, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable AdInfoLookup.AdInfo input) {
                        return Optional.fromNullable(input.getTitle()).or("");
                    }
                }).or(""))
                .put("isRobot", isRobot.toString()).build();
    }

    private Map<String, String> apnsDetails(final Conversation conversation, Message message, Optional<AdInfoLookup.AdInfo> adInfo, Boolean isRobot) {
        String messageBody = messageShort(message);
        ImmutableMap.Builder<String,String> builder = ImmutableMap.<String,String>builder()
                .put("senderInfo", senderInfo(message, conversation))
                .put("senderMessage", messageBody)
                .put("adImageUrl", adInfo.transform(new Function<AdInfoLookup.AdInfo, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable AdInfoLookup.AdInfo input) {
                        return Optional.fromNullable(input.getImageUrl()).or("");
                    }
                }).or(""))
                .put("title",messageBody)
                .put("isRobot", isRobot.toString());

        JSONObject ebay = new JSONObject();
        ebay.put("AdImageUrl",adInfo.transform(new Function<AdInfoLookup.AdInfo, String>() {
            @Nullable
            @Override
            public String apply(@Nullable AdInfoLookup.AdInfo input) {
                return Optional.fromNullable(input.getImageUrl()).or("");
            }
        }).or(""));
        ebay.put("AdSubject", adInfo.transform(new Function<AdInfoLookup.AdInfo, String>() {
            @Nullable
            @Override
            public String apply(@Nullable AdInfoLookup.AdInfo input) {
                return Optional.fromNullable(input.getTitle()).or("");
            }
        }).or(""));
        ebay.put("Type", "3");
        ebay.put("ConversationId", conversation.getId());
        ebay.put("AdId", conversation.getAdId());
        builder.put("ebay",ebay.toString());

        return builder.build();
    }

    private String createPushMessageText(Conversation conversation, Message message) {
        LOG.debug("Sender information for push: "+senderInfo(message,conversation));
        return MessageContentHelper.senderName(senderInfo(message, conversation)) + ": " + messageShort(message);
    }

    private String senderInfo(Message message, Conversation conversation) {
        if (MessageType.isRobot(message)) {
            return GUMTREE_SENDER;
        } else {
            if (MessageDirection.BUYER_TO_SELLER.equals(message.getMessageDirection())) {
                return Optional.fromNullable(conversation.getCustomValues().get("buyer-name")).or("Buyer");
            }

            if (MessageDirection.SELLER_TO_BUYER.equals(message.getMessageDirection())) {
                return Optional.fromNullable(conversation.getCustomValues().get("seller-name")).or("Seller");
            }
        }

        LOG.error("Undefined MessageDirection for message " + message.getId());
        return "";

    }

    private String messageShort(Message message) {
        String shortText = TextCleaner.cleanupText(message.getPlainTextBody());

        if (shortText != null) {
            return truncateText(shortText, 50);
        }
        return "";

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
