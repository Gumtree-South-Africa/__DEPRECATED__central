package com.ecg.messagecenter.kjca.pushmessage;

import com.ecg.comaas.kjca.coremod.shared.TraceThreadLocal;
import com.ecg.comaas.kjca.coremod.shared.BoxHeaders;
import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.core.cleanup.kjca.TextCleaner;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.capi.AdInfoLookup;
import com.ecg.messagecenter.kjca.capi.UserInfoLookup;
import com.ecg.messagecenter.kjca.persistence.SimplePostBoxInitializer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PushMessageOnUnreadConversationCallback implements SimplePostBoxInitializer.PostBoxWriteCallback {

    private static final String FROM = "From";
    private static final String SOUND_FILE_NAME = "kijijica-push.caf";
    private static final int MAX_TITLE_LENGTH = 250;

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageOnUnreadConversationCallback.class);
    private static final Locale DEFAULT_LOCALE = Locale.CANADA;
    private static final Pattern DISPLAY_NAME_REMOVE_QUOTES = Pattern.compile("^\"([^\"]+)\"$");

    private final SimplePostBoxRepository postBoxRepository;
    private final PushService amqPushService;
    private final PushService sendPushService;
    private final TextAnonymizer textAnonymizer;
    private final AdInfoLookup adInfoLookup;
    private final UserInfoLookup userInfoLookup;
    private final Conversation conversation;
    private final Message message;
    private final Integer sendPushPercentage;

    public PushMessageOnUnreadConversationCallback(
            SimplePostBoxRepository postBoxRepository,
            Integer sendPushPercentage,
            PushService amqPushService,
            PushService sendPushService,
            TextAnonymizer textAnonymizer,
            AdInfoLookup adInfoLookup,
            UserInfoLookup userInfoLookup,
            Conversation conversation,
            Message message) {
        this.postBoxRepository = postBoxRepository;
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

    private static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

    @Override
    public void success(String email, Long unreadCount, boolean markedAsUnread) {
        // only push-trigger on unread use-case

        if (!markedAsUnread) {
            return;
        }

        sendPushMessage(email);
    }

    private void sendPushMessage(String email) {
        PushService pushService = null;
        try {
            TraceThreadLocal.set(UUID.randomUUID().toString());
            Optional<AdInfoLookup.AdInfo> adInfo = adInfoLookup.lookupInfo("adId", conversation.getAdId());
            Optional<UserInfoLookup.UserInfo> userInfo = userInfoLookup.lookupInfo("email", email);
            int unreadCount = getUnreadConversationCount(email);

            Optional<PushMessagePayload> payload = createPayloadBasedOnNotificationRules(conversation, message, email, unreadCount, adInfo, userInfo);
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

    private int getUnreadConversationCount(String email) {
        final PostBox<AbstractConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        return postBox.getUnreadConversations().size();
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

    private Optional<PushMessagePayload> createPayloadBasedOnNotificationRules(Conversation conversation, Message message, String email, int unreadCount, Optional<AdInfoLookup.AdInfo> adInfo, Optional<UserInfoLookup.UserInfo> userInfo) {
        if (!userInfo.isPresent()) {
            return Optional.empty();
        }

        String pushMessage = createPushMessageText(message, getConversationLocale(conversation));
        return Optional.of(
                new PushMessagePayload(
                        email,
                        userInfo.map(UserInfoLookup.UserInfo::getUserId).orElse(""),
                        pushMessage,
                        "CHATMESSAGE",
                        Optional.of(details(conversation, adInfo, pushMessage, unreadCount)),
                        Optional.of(unreadCount)
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
                                        long unreadConversationCount) {
        return ImmutableMap.<String, String>builder()
                .put("sound", SOUND_FILE_NAME)
                .put("AdImageUrl", adInfo.map(AdInfoLookup.AdInfo::getImageUrl).orElse(""))
                .put("title", StringUtils.left(pushMessage, MAX_TITLE_LENGTH))
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
