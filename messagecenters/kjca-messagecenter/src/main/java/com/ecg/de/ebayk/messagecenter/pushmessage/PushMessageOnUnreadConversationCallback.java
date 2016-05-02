package com.ecg.de.ebayk.messagecenter.pushmessage;

import ca.kijiji.replyts.BoxHeaders;
import com.codahale.metrics.Counter;
import com.ecg.de.ebayk.messagecenter.capi.AdInfoLookup;
import com.ecg.de.ebayk.messagecenter.capi.UserInfoLookup;
import com.ecg.de.ebayk.messagecenter.cleanup.TextCleaner;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxInitializer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PushMessageOnUnreadConversationCallback implements PostBoxInitializer.PostBoxWriteCallback {

    private static final String FROM = "From";
    private static final String SOUND_FILE_NAME = "kijijica-push.caf";

    private static final Counter COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.push-message-sent");
    private static final Counter COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.push-message-no-device");
    private static final Counter COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.push-message-failed");

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);
    private static final Locale DEFAULT_LOCALE = Locale.CANADA;
    private static final Pattern DISPLAY_NAME_REMOVE_QUOTES = Pattern.compile("^\"([^\"]+)\"$");

    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;
    private final UserInfoLookup userInfoLookup;
    private final Conversation conversation;
    private final Message message;

    public PushMessageOnUnreadConversationCallback(
            PushService pushService,
            AdInfoLookup adInfoLookup,
            UserInfoLookup userInfoLookup,
            Conversation conversation,
            Message message) {

        this.pushService = pushService;
        this.adInfoLookup = adInfoLookup;
        this.userInfoLookup = userInfoLookup;
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
            Optional<AdInfoLookup.AdInfo> adInfo = adInfoLookup.lookupAdInfo(Long.parseLong(conversation.getAdId()));
            Optional<UserInfoLookup.UserInfo> userInfo = userInfoLookup.lookupUserInfo(postBox.getEmail());

            Optional<PushMessagePayload> payload = createPayloadBasedOnNotificationRules(conversation, message, postBox, adInfo, userInfo);

            if (!payload.isPresent()) {
                return;
            }

            LOG.debug("Sending push message, Payload:[{}]", payload.get().asJson());
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
                LOG.error("Error sending push for conversation '{}' and message '{}'", conversation.getId(), message.getId(), sendPushResult.getException());
            }

        } catch (Exception e) {
            COUNTER_PUSH_FAILED.inc();
            LOG.error("Error sending push for conversation '{}' and message '{}'", conversation.getId(), message.getId(), e);
        }
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
        String shortText = TextCleaner.cleanupText(message.getPlainTextBody());

        if (shortText != null) {
            return shortText;
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
