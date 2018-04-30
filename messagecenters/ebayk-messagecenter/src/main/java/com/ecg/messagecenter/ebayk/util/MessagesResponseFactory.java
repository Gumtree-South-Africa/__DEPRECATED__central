package com.ecg.messagecenter.ebayk.util;

import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.core.util.MessageCenterUtils;
import com.ecg.messagecenter.ebayk.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class MessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesResponseFactory.class);


    private static final Histogram TEXT_SIZE_CONVERSATIONS = TimingReports.newHistogram("message-box.text-size-conversations");
    private static final Histogram TEXT_SIZE_MESSAGES = TimingReports.newHistogram("message-box.text-size-messages");

    static final String BUYER_PHONE_FIELD = "buyer-phonenumber";

    private MessagesDiffer differ;

    public MessagesResponseFactory(MessagesDiffer differ) {
        this.differ = differ;
    }

    public Optional<MessageResponse> latestMessage(String email, Conversation conv) {

        List<Message> filtered = filterMessages(conv.getMessages(), conv, email);

        if (filtered.size() == 0) {
            return Optional.empty();
        }

        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);

        List<MessageResponse> transformedMessages = new ArrayList<MessageResponse>();
        if (filtered.size() == 1) {
            addIntialContactPosterMessage(conv, role, transformedMessages, filtered.get(0));
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


        if (transformedMessages.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(Iterables.getLast(transformedMessages));
        }
    }

    public Optional<List<MessageResponse>> create(String email, Conversation conv, List<Message> messageRts) {
        List<Message> filtered = filterMessages(messageRts, conv, email);


        if (filtered.size() == 0) {
            return Optional.empty();
        }

        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);

        List<MessageResponse> transformedMessages = new ArrayList<MessageResponse>();

        addIntialContactPosterMessage(conv, role, transformedMessages, filtered.get(0));
        addReplyMessages(role, conv, filtered, transformedMessages);

        if (transformedMessages.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(transformedMessages);
        }
    }

    private List<Message> filterMessages(List<Message> messageRts, Conversation conv, String email) {
        List<Message> filtered = new ArrayList<Message>();
        for (Message item : messageRts) {
            if (item.getState() != MessageState.IGNORED && shouldBeIncluded(item, conv, email)) {
                filtered.add(item);
            }
        }


        return filtered;
    }


    private void addIntialContactPosterMessage(Conversation conv, ConversationRole role, List<MessageResponse> transformedMessages, Message firstMessage) {
        String phoneNumber = conv.getCustomValues().get(BUYER_PHONE_FIELD);

        transformedMessages.add(
                new MessageResponse(
                        firstMessage.getId(),
                        MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(firstMessage.getReceivedAt()),
                        firstMessage.getHeaders().get("X-Offerid"),
                        ConversationBoundnessFinder.boundnessForRole(role, firstMessage.getMessageDirection()),
                        differ.cleanupFirstMessage(firstMessage.getPlainTextBody()),
                        Optional.ofNullable(phoneNumber),
                        MessageResponse.Attachment.transform(firstMessage),
                        conv.getBuyerId()));
    }


    private void addReplyMessages(ConversationRole role, Conversation conv, List<Message> messageRts, List<MessageResponse> transformedMessages) {

        // start with '1' we handled first message with cleanup above
        for (int i = 1; i < messageRts.size(); i++) {

            String diffedMessage;
            Message message = messageRts.get(i);

            if (contactPosterForExistingConversation(message)) {
                // new contactPoster from payload point of view same as first message of a conversation
                diffedMessage = differ.cleanupFirstMessage(message.getPlainTextBody());
            } else if (comesFromMessageBoxClient(message)) {
                // no need to strip or diff anything, in message-box the whole payload is a additional message
                diffedMessage = new MessagePreProcessor().removeFromMessageboxReply(message.getPlainTextBody().trim());
            } else {
                MessagesDiffer.DiffInput left = new MessagesDiffer.DiffInput(lookupMessageToBeDiffedWith(messageRts, i).getPlainTextBody(), conv.getId(), lookupMessageToBeDiffedWith(messageRts, i).getId());
                MessagesDiffer.DiffInput right = new MessagesDiffer.DiffInput(message.getPlainTextBody(), conv.getId(), message.getId());

                int textSize = left.getText().length() + right.getText().length();
                TEXT_SIZE_MESSAGES.update(textSize);
                if (textSize > 200000) {
                    LOG.info("Large message-diff text-set KB: '{}', conv #{}", textSize, conv.getId());
                }

                TextDiffer.TextCleanerResult cleanupResult = differ.diff(left, right);

                diffedMessage = cleanupResult.getCleanupResult();
            }


            transformedMessages.add(
                    new MessageResponse(
                            message.getId(),
                            MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedAt()),
                            message.getHeaders().get("X-Offerid"),
                            ConversationBoundnessFinder.boundnessForRole(role, message.getMessageDirection()),
                            diffedMessage,
                            Optional.<String>empty(),
                            MessageResponse.Attachment.transform(message),
                            message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? conv.getBuyerId() : conv.getSellerId()
                    )
            );

        }
    }

    private Message lookupMessageToBeDiffedWith(List<Message> messageRts, int i) {
        // unlikely that reply is from own message, rather refer to last message of other party
        MessageDirection messageDirection = messageRts.get(i).getMessageDirection();
        for (int j = 1; i - j >= 0; j++) {
            if (messageDirection == MessageDirection.BUYER_TO_SELLER) {
                if (messageRts.get(i - j).getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
                    return messageRts.get(i - j);
                }
            }
            if (messageDirection == MessageDirection.SELLER_TO_BUYER) {
                if (messageRts.get(i - j).getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
                    return messageRts.get(i - j);
                }
            }
        }

        // fallback first message tried to be diffed
        return messageRts.get(0);
    }

    private boolean shouldBeIncluded(Message messageRts, Conversation conversationRts, String email) {
        return messageRts.getState() == MessageState.SENT || isOwnMessage(email, conversationRts, messageRts);
    }

    private boolean isOwnMessage(String email, Conversation conversationRts, Message messageRts) {
        if (messageRts.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
            if (conversationRts.getBuyerId().toLowerCase().equals(email.toLowerCase())) {
                return true;
            }
        }
        if (messageRts.getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
            if (conversationRts.getSellerId().toLowerCase().equals(email.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean comesFromMessageBoxClient(Message messageRts) {
        return messageRts.getHeaders().containsKey("X-Reply-Channel") &&
                (messageRts.getHeaders().get("X-Reply-Channel").contains("api_") ||
                        messageRts.getHeaders().get("X-Reply-Channel").contains("desktop"));
    }

    private boolean contactPosterForExistingConversation(Message messageRts) {
        return messageRts.getHeaders().containsKey("X-Reply-Channel") &&
                messageRts.getHeaders().get("X-Reply-Channel").startsWith("cp_");
    }

}
