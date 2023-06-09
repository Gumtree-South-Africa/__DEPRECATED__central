package com.ecg.messagecenter.gtau.util;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.core.cleanup.gtau.TextCleaner;
import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.core.util.MessageCenterUtils;
import com.ecg.messagecenter.gtau.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesResponseFactory.class);
    private static final String REPLY_CHANNEL = "X-Reply-Channel";
    private static final String BUYER_PHONE_FIELD = "buyer-phonenumber";
    private static final Timer CLEANUP_TIMER = TimingReports.newTimer("message-box.cleanup-timer");
    private static final Histogram TEXT_SIZE_CONVERSATIONS = TimingReports.newHistogram("message-box.text-size-conversations");

    private MessagesResponseFactory() {
    }

    public static Optional<MessageResponse> latestMessage(String email, Conversation conv) {
        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);
        List<Message> filtered = filterMessages(conv.getMessages(), conv, email, role);

        if (filtered.isEmpty()) {
            return Optional.empty();
        }

        List<MessageResponse> transformedMessages = new ArrayList<>();
        if (filtered.size() == 1) {
            addInitialContactPosterMessage(conv, role, transformedMessages, filtered.get(0));
        } else {
            Message last = filtered.get(filtered.size() - 1);
            Message secondLast = lookupMessageToBeDiffedWith(filtered, filtered.size() - 1);

            int textSize = secondLast.getPlainTextBody().length() + last.getPlainTextBody().length();
            TEXT_SIZE_CONVERSATIONS.update(textSize);
            if (textSize > 200000) {
                LOG.info("Large conversation text-set: KB '{}', conv #{} email '{}'", textSize, conv.getId(), conv.getBuyerId());
            }

            addReplyMessages(role, conv, Arrays.asList(secondLast, last), transformedMessages);
        }

        return transformedMessages.isEmpty() ? Optional.empty() : Optional.of(Iterables.getLast(transformedMessages));
    }

    public static Optional<List<MessageResponse>> create(String email, Conversation conv, List<Message> messageRts) {
        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);
        List<Message> filtered = filterMessages(messageRts, conv, email, role);

        if (filtered.isEmpty()) {
            return Optional.empty();
        }

        List<MessageResponse> transformedMessages = new ArrayList<>();

        addInitialContactPosterMessage(conv, role, transformedMessages, filtered.get(0));
        addReplyMessages(role, conv, filtered, transformedMessages);

        return transformedMessages.isEmpty() ? Optional.empty() : Optional.of(transformedMessages);
    }

    private static List<Message> filterMessages(List<Message> messageRts, Conversation conv, String email, ConversationRole role) {
        return messageRts.stream()
                .filter(message -> message.getState() != MessageState.IGNORED && shouldBeIncluded(message, conv, email, role))
                .collect(Collectors.toList());
    }

    private static boolean shouldBeIncluded(Message messageRts, Conversation conversationRts, String email, ConversationRole role) {
        return (messageRts.getState() == MessageState.SENT || isOwnMessage(email, conversationRts, messageRts)) && includeRobotMessage(messageRts, role);
    }

    private static boolean includeRobotMessage(Message messageRts, ConversationRole role) {
        if (MessageType.isRobot(messageRts)) {
            if ((messageRts.getMessageDirection().equals(MessageDirection.SELLER_TO_BUYER) && role.equals(ConversationRole.Seller))
                    || (messageRts.getMessageDirection().equals(MessageDirection.BUYER_TO_SELLER) && role.equals(ConversationRole.Buyer))) {
                return false;
            }
        }
        return true;
    }

    private static void addInitialContactPosterMessage(Conversation conv, ConversationRole role, List<MessageResponse> transformedMessages, Message firstMessage) {
        String phoneNumber = conv.getCustomValues().get(BUYER_PHONE_FIELD);

        transformedMessages.add(
                new MessageResponse(
                        MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(firstMessage.getReceivedAt()),
                        Optional.ofNullable(MessageType.getOffer(firstMessage)),
                        Optional.ofNullable(MessageType.getRobot(firstMessage)),
                        ConversationBoundnessFinder.boundnessForRole(role, firstMessage.getMessageDirection()),
                        cleanupFirstMessage(firstMessage.getPlainTextBody()),
                        Optional.ofNullable(phoneNumber),
                        MessageResponse.Attachment.transform(firstMessage),
                        conv.getBuyerId(),
                        MessageType.getLinks(firstMessage),
                        MessageType.getRobotDetails(firstMessage)));
    }

    private static void addReplyMessages(ConversationRole role, Conversation conv, List<Message> messageRts, List<MessageResponse> transformedMessages) {

        // start with '1' we handled first message with cleanup above
        for (int i = 1; i < messageRts.size(); i++) {
            String diffedMessage;

            if (contactPosterForExistingConversation(messageRts.get(i))) {
                // new contactPoster from payload point of view same as first message of a conversation
                diffedMessage = cleanupFirstMessage(messageRts.get(i).getPlainTextBody());
            } else if (comesFromMessageBoxClient(messageRts.get(i))) {
                // no need to strip or diff anything, in message-box the whole payload is a additional message
                diffedMessage = messageRts.get(i).getPlainTextBody().trim();
            } else {
                diffedMessage = cleanupFirstMessage(messageRts.get(i).getPlainTextBody().trim());
            }

            transformedMessages.add(
                    new MessageResponse(
                            MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(messageRts.get(i).getReceivedAt()),
                            Optional.ofNullable(MessageType.getOffer(messageRts.get(i))),
                            Optional.ofNullable(MessageType.getRobot(messageRts.get(i))),
                            ConversationBoundnessFinder.boundnessForRole(role, messageRts.get(i).getMessageDirection()),
                            diffedMessage,
                            Optional.empty(),
                            MessageResponse.Attachment.transform(messageRts.get(i)),
                            messageRts.get(i).getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? conv.getBuyerId() : conv.getSellerId(),
                            MessageType.getLinks(messageRts.get(i)),
                            MessageType.getRobotDetails(messageRts.get(i)))
            );
        }
    }

    private static Message lookupMessageToBeDiffedWith(List<Message> messageRts, int i) {
        // unlikely that reply is from own message, rather refer to last message of other party
        MessageDirection messageDirection = messageRts.get(i).getMessageDirection();
        for (int j = 1; i - j >= 0; j++) {
            if (messageDirection == MessageDirection.BUYER_TO_SELLER && messageRts.get(i - j).getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
                return messageRts.get(i - j);
            }
            if (messageDirection == MessageDirection.SELLER_TO_BUYER && messageRts.get(i - j).getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
                return messageRts.get(i - j);
            }
        }

        // fallback first message tried to be diffed
        return messageRts.get(0);
    }

    private static boolean isOwnMessage(String email, Conversation conversationRts, Message messageRts) {
        return messageRts.getMessageDirection() == MessageDirection.BUYER_TO_SELLER && conversationRts.getBuyerId().equalsIgnoreCase(email)
                || messageRts.getMessageDirection() == MessageDirection.SELLER_TO_BUYER && conversationRts.getSellerId().equalsIgnoreCase(email);
    }

    private static boolean comesFromMessageBoxClient(Message messageRts) {
        return messageRts.getCaseInsensitiveHeaders().containsKey(REPLY_CHANNEL) &&
                (messageRts.getCaseInsensitiveHeaders().get(REPLY_CHANNEL).contains("api") ||
                        messageRts.getCaseInsensitiveHeaders().get(REPLY_CHANNEL).contains("desktop"));
    }

    private static boolean contactPosterForExistingConversation(Message messageRts) {
        return messageRts.getCaseInsensitiveHeaders().containsKey(REPLY_CHANNEL) &&
                messageRts.getCaseInsensitiveHeaders().get(REPLY_CHANNEL).startsWith("cp_");
    }

    private static String cleanupFirstMessage(String firstMessage) {
        try (Timer.Context ignored = CLEANUP_TIMER.time()) {
            return TextCleaner.cleanupText(firstMessage);
        }
    }
}
