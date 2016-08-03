package com.ecg.messagecenter.pushmessage;

import ca.kijiji.replyts.BoxHeaders;
import ca.kijiji.replyts.TextAnonymizer;
import ca.kijiji.tracing.TraceThreadLocal;
import com.ecg.messagecenter.capi.AdInfoLookup;
import com.ecg.messagecenter.capi.UserInfoLookup;
import com.ecg.messagecenter.cleanup.TextCleaner;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.SimplePostBoxInitializer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PushMessageOnUnreadConversationCallback implements SimplePostBoxInitializer.PostBoxWriteCallback {

    private static final String FROM = "From";
    private static final String SOUND_FILE_NAME = "kijijica-push.caf";

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);
    private static final Locale DEFAULT_LOCALE = Locale.CANADA;
    private static final Pattern DISPLAY_NAME_REMOVE_QUOTES = Pattern.compile("^\"([^\"]+)\"$");

    private final PushService amqPushService;
    private final PushService sendPushService;
    private final TextAnonymizer textAnonymizer;
    private final AdInfoLookup adInfoLookup;
    private final UserInfoLookup userInfoLookup;
    private final Conversation conversation;
    private final Message message;
    private final Integer sendPushPercentage;

    public PushMessageOnUnreadConversationCallback(
            Integer sendPushPercentage,
            PushService amqPushService,
            PushService sendPushService,
            TextAnonymizer textAnonymizer,
            AdInfoLookup adInfoLookup,
            UserInfoLookup userInfoLookup,
            Conversation conversation,
            Message message) {
        this.sendPushPercentage = sendPushPercentage;
        this.amqPushService = amqPushService;
        this.sendPushService = sendPushService;
        this.textAnonymizer = textAnonymizer;
        this.adInfoLookup = adInfoLookup;
        this.userInfoLookup = userInfoLookup;
        this.conversation = conversation;
        this.message = message;
    }

    static String truncateText(String description, int maxChars) {
        if (isNullOrEmpty(description)) {
            return "";
        }

        if (description.length() <= maxChars) {
            return description;
        }

        String substring = description.substring(0, maxChars);
        return substringBeforeLast(substring, " ").concat("...");
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

    @Override
    public void success(PostBox postBox, boolean markedAsUnread) {

        // only push-trigger on unread use-case
        if (!markedAsUnread) {
            return;
        }

        sendPushMessage(postBox);
    }

    void sendPushMessage(PostBox postBox) {
        PushService pushService = null;
        try {
            TraceThreadLocal.set(UUID.randomUUID().toString());
            Optional<AdInfoLookup.AdInfo> adInfo = adInfoLookup.lookupAdInfo(Long.parseLong(conversation.getAdId()));
            Optional<UserInfoLookup.UserInfo> userInfo = userInfoLookup.lookupUserInfo(postBox.getEmail());

            Optional<PushMessagePayload> payload = createPayloadBasedOnNotificationRules(conversation, message, postBox, adInfo, userInfo);

            if (!payload.isPresent()) {
                return;
            }

            pushService = determinePushService();
            sendPushMessageInternal(pushService, payload.get());

        } catch (Exception e) {
            if (pushService != null) {
                pushService.incrementCounter(PushService.Result.Status.ERROR);
            }
            LOG.error("Error sending push for conversation '{}' and message '{}'", conversation.getId(), message.getId(), e);
        }
    }

    private void sendPushMessageInternal(PushService pushService, PushMessagePayload payload) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending push message, Payload:[{}]", payload.asJson());
        }
        PushService.Result sendPushResult = pushService.sendPushMessage(payload);

        pushService.incrementCounter(sendPushResult.getStatus());

        if (PushService.Result.Status.ERROR == sendPushResult.getStatus()) {
            LOG.error("Error sending push for conversation '{}' and message '{}'", conversation.getId(), message.getId(), sendPushResult.getException());
        }

    }

    /**
     * Determines which push service to use, this is for tuning and rolling out sending push notifications via SEND API.
     *
     * @return the right push service to be used.
     */
    private PushService determinePushService() {
        if (sendPushPercentage == null || Math.random() * 100 >= sendPushPercentage) {
            LOG.debug("No send service percentage is defined or random number is larger than the allowed percentage. Falling back to using old push service.");
            return amqPushService;
        }
        LOG.debug("Going to send message thru SEND");
        return sendPushService;
    }

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, PostBox postBox, Optional<AdInfoLookup.AdInfo> adInfo, Optional<UserInfoLookup.UserInfo> userInfo) {
        if (!userInfo.isPresent()) {
            return Optional.empty();
        }

        String pushMessage = createPushMessageText(message, getConversationLocale(conversation));
        final int unreadConversationCount = postBox.getUnreadConversations().size();
        return Optional.of(
                new PushMessagePayload(
                        postBox.getEmail(),
                        userInfo.map(UserInfoLookup.UserInfo::getUserId).orElse(""),
                        pushMessage,
                        "CHATMESSAGE",
                        Optional.of(details(conversation, adInfo, pushMessage, unreadConversationCount)),
                        Optional.of(unreadConversationCount)
                )
        );
    }

    private Locale getConversationLocale(final Conversation conversation) {
        final List<Message> messages = conversation.getMessages();
        if (messages.isEmpty()) {
            return DEFAULT_LOCALE;
        }

        final String locale = messages.get(0).getHeaders().get(BoxHeaders.LOCALE.getHeaderName());
        return locale != null ? Locale.forLanguageTag(locale.replace('_', '-')) : DEFAULT_LOCALE; // X-Process-Locale does not pass a BCP 47-compliant language-country string, and needs to be modified
    }

    private Map<String, String> details(final Conversation conversation,
                                        Optional<AdInfoLookup.AdInfo> adInfo,
                                        String pushMessage,
                                        int unreadConversationCount) {
        return ImmutableMap.<String, String>builder()
                .put("sound", SOUND_FILE_NAME)
                .put("AdImageUrl", adInfo.map(AdInfoLookup.AdInfo::getImageUrl).orElse(""))
                .put("title", pushMessage)
                .put("AdId", conversation.getAdId())
                .put("ConversationId", conversation.getId())
                .put("AdSubject", adInfo.map(AdInfoLookup.AdInfo::getTitle).orElse(""))
                .put("badge", String.valueOf(unreadConversationCount))
                .build();
    }

    private String createPushMessageText(final Message message, final Locale locale) {
        final String senderInfo = senderName(message, locale);
        LOG.debug("Sender information for push: {}", senderInfo);
        return senderInfo + ": " + messageShort(message);
    }

    private String senderName(final Message message, final Locale locale) {
        final String from = message.getHeaders().get(FROM);

        if (senderHasNameAndEmailAddress(from)) {
            final String emailDisplayName = from.substring(0, from.lastIndexOf('<')).trim();
            final Matcher matcher = DISPLAY_NAME_REMOVE_QUOTES.matcher(emailDisplayName);
            return matcher.find() ? matcher.group(1) : emailDisplayName;
        }

        return Locale.CANADA_FRENCH.equals(locale) ? "Utilisateur Kijiji" : "Kijiji User";
    }

    private boolean senderHasNameAndEmailAddress(final String from) {
        return from.contains(" <");
    }

    private String messageShort(Message message) {
        String shortText = TextCleaner.cleanupText(textAnonymizer.anonymizeText(conversation, message.getPlainTextBody()));

        if (shortText != null) {
            return shortText;
        }
        return "";

    }
}
