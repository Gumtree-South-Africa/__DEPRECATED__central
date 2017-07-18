package com.ecg.messagecenter.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.cleanup.TextCleaner;
import com.ecg.messagecenter.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.util.MessageContentHelper;
import com.ecg.messagecenter.util.MessageType;
import com.ecg.messagecenter.webapi.responses.ResponseUtil;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class PushMessageOnUnreadConversationCallback implements SimplePostBoxInitializer.PostBoxWriteCallback {
    private static final Counter COUNTER_PUSH_SENT =
                    TimingReports.newCounter("message-box.push-message-sent");
    private static final Counter COUNTER_PUSH_NO_DEVICE =
                    TimingReports.newCounter("message-box.push-message-no-device");
    private static final Counter COUNTER_PUSH_FAILED =
                    TimingReports.newCounter("message-box.push-message-failed");

    private static final String GUMTREE_SENDER = "The Gumtree Team";

    private static final Logger LOG =
                    LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);

    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final Conversation conversation;
    private final Message message;

    public PushMessageOnUnreadConversationCallback(PushService pushService,
                    AdInfoLookup adInfoLookup, Conversation conversation, Message message) {

        this.pushService = pushService;
        this.adInfoLookup = adInfoLookup;
        this.conversation = conversation;
        this.message = message;
    }

    @Override
    public void success(String email, Long unreadCount, boolean markedAsUnread) {
        // only push-trigger on unread use-case
        if (markedAsUnread) {
            sendPushMessage(email, unreadCount.intValue());
        }
    }

    void sendPushMessage(String email, Integer unreadCount) {
        try {
            Optional<AdInfoLookup.AdInfo> adInfo =
                            adInfoLookup.lookupAdIInfo(conversation.getAdId());

            Optional<PushMessagePayload> payload =
                            createPayloadBasedOnNotificationRules(conversation, message, email, unreadCount,
                                            adInfo);

            if (!payload.isPresent()) {
                return;
            }

            LOG.debug("Sending push message, Payload:[" + payload.get().asJson() + "]");
            PushService.Result sendPushResult = PushService.Result.ok(null);
            if (pushService != null) {
                sendPushResult = pushService.sendPushMessage(payload.get());
            }

            if (PushService.Result.Status.OK == sendPushResult.getStatus()) {
                LOG.info("sent push messsage succesfully");
                COUNTER_PUSH_SENT.inc();
            } else if (PushService.Result.Status.NOT_FOUND == sendPushResult.getStatus()) {
                LOG.info(format("Not found sending push for conversation '%s' and message '%s'",
                                conversation.getId(), message.getId()));
                COUNTER_PUSH_NO_DEVICE.inc();
            } else if (PushService.Result.Status.ERROR == sendPushResult.getStatus()) {
                COUNTER_PUSH_FAILED.inc();
                LOG.error(format("Error sending push for conversation '%s' and message '%s'",
                                conversation.getId(), message.getId()),
                                sendPushResult.getException().get());
            } else {
                LOG.error(format("Unkown status returned from push service status: '%s'",
                                sendPushResult.getStatus().toString()));
            }

        } catch (Exception e) {
            COUNTER_PUSH_FAILED.inc();
            LOG.error(format("Error sending push for conversation '%s' and message '%s'",
                            conversation.getId(), message.getId()), e);
        }
    }

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(
                    Conversation conversation, Message message, String email, Integer unreadCount,
                    Optional<AdInfoLookup.AdInfo> adInfo) {
        String pushMessage = createPushMessageText(conversation, message);
        return Optional.of(new PushMessagePayload(email, pushMessage, "CHATMESSAGE",
                                        ImmutableMap.of("ConversationId", conversation.getId()),
                                        Optional.of(unreadCount),
                                        Optional.of(gcmDetails(conversation, message, adInfo,
                                                        pushMessage, MessageType.isRobot(message))),
                                        Optional.of(apnsDetails(conversation, message, adInfo,
                                                        MessageType.isRobot(message))),
                                        Optional.of(notificationDetails(conversation, message,
                                                        adInfo, MessageType.isRobot(message)))));
    }

    private Map<String, String> notificationDetails(Conversation conversation, Message message,
                    Optional<AdInfoLookup.AdInfo> adInfo, boolean robot) {
        String messageBody = messageShort(message);
        return ImmutableMap.<String, String>builder().put("body",
                        MessageContentHelper.senderName(this.senderInfo(message, conversation))
                                        + ": " + messageBody)
                        .put("title", adInfo.map((x) -> x.getTitle()).orElse(""))
                        .put("action-loc-key", "strViewBtn").put("badge", "1")
                        .put("sound", "default").build();
    }

    private Map<String, String> gcmDetails(final Conversation conversation, Message message,
                    Optional<AdInfoLookup.AdInfo> adInfo, String pushMessage, Boolean isRobot) {
        String messageBody = messageShort(message);
        return ImmutableMap.<String, String>builder()
                        .put("senderInfo", senderInfo(message, conversation))
                        .put("senderMessage", messageBody).put("AdImageUrl", adInfo.map((x) -> x.getImageUrl()).orElse(""))
                        .put("title", pushMessage).put("type", "3")
                        .put("AdId", String.valueOf(adInfoLookup.getAdIdFrom(conversation.getAdId())))
                        .put("AdSubject", adInfo.map((x) -> "Kijiji: " + x.getTitle()).orElse(""))
                        .put("isRobot", isRobot.toString()).build();
    }

    private Map<String, String> apnsDetails(final Conversation conversation, Message message,
                    Optional<AdInfoLookup.AdInfo> adInfo, Boolean isRobot) {
        String messageBody = messageShort(message);
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                        .put("senderInfo", senderInfo(message, conversation))
                        .put("senderMessage", messageBody).put("adImageUrl", adInfo.map((x) -> x.getImageUrl()).orElse(""))
                        .put("isRobot", isRobot.toString());

        JSONObject ebay = new JSONObject();
        ebay.put("AdImageUrl", adInfo.map((x) -> x.getImageUrl()).orElse(""));
        ebay.put("AdSubject", adInfo.map((x) -> x.getTitle()).orElse(""));
        ebay.put("Type", "3");
        ebay.put("ConversationId", conversation.getId());
        ebay.put("AdId", String.valueOf(adInfoLookup.getAdIdFrom(conversation.getAdId())));
        builder.put("ebay", ebay.toString());

        return builder.build();
    }

    private String createPushMessageText(Conversation conversation, Message message) {
        LOG.debug("Sender information for push: " + senderInfo(message, conversation));
        return MessageContentHelper.senderName(senderInfo(message, conversation)) + ": "
                        + messageShort(message);
    }

    private String senderInfo(Message message, Conversation conversation) {
        if (MessageType.isRobot(message)) {
            return GUMTREE_SENDER;
        } else {
            if (MessageDirection.BUYER_TO_SELLER.equals(message.getMessageDirection())) {
                return Optional.ofNullable(conversation.getCustomValues().get("buyer-name"))
                                .orElse(getNameFromMessages(conversation.getMessages(),
                                                MessageDirection.BUYER_TO_SELLER));
            }

            if (MessageDirection.SELLER_TO_BUYER.equals(message.getMessageDirection())) {
                return Optional.ofNullable(conversation.getCustomValues().get("seller-name"))
                                .orElse(getNameFromMessages(conversation.getMessages(),
                                                MessageDirection.SELLER_TO_BUYER));
            }
        }

        LOG.error("Undefined MessageDirection for message " + message.getId());
        return "";

    }

    private String getNameFromMessages(List<Message> messages, MessageDirection direction) {
        return ResponseUtil.getName(messages, direction);
    }

    private String messageShort(Message message) {
        return TextCleaner.cleanupText(message.getPlainTextBody());
    }
}
