package com.ecg.de.ebayk.messagecenter.util;

import com.codahale.metrics.Histogram;
import com.ecg.de.ebayk.messagecenter.persistence.Header;
import com.ecg.de.ebayk.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class MessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesResponseFactory.class);


    private static final Histogram TEXT_SIZE_CONVERSATIONS = TimingReports.newHistogram("message-box.text-size-conversations");
    private static final Histogram TEXT_SIZE_MESSAGES = TimingReports.newHistogram("message-box.text-size-messages");
    private static final String REPLY_CHANNEL = "X-Reply-Channel";

    static final String BUYER_PHONE_FIELD = "buyer-phonenumber";

    private MessagesDiffer differ;

    public MessagesResponseFactory(MessagesDiffer differ) {
        this.differ = differ;
    }

    public Optional<MessageResponse> latestMessage(String email, Conversation conv) {

        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);
        List<Message> filtered = filterMessages(conv.getMessages(), conv, email, role, true);

        if (filtered.size() == 0) {
            return Optional.absent();
        }

        List<MessageResponse> transformedMessages = new ArrayList<>();
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
            return Optional.absent();
        } else {
            return Optional.of(Iterables.getLast(transformedMessages));
        }
    }

    public Optional<List<MessageResponse>> create(String email, Conversation conv, List<Message> messageRts) {
        return create(email, conv, messageRts, true);
    }

    public Optional<List<MessageResponse>> create(String email, Conversation conv, List<Message> messageRts, boolean robotEnabled) {
        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);
        List<Message> filtered = filterMessages(messageRts, conv, email, role, robotEnabled);

        if (filtered.size() == 0) {
            return Optional.absent();
        }

        List<MessageResponse> transformedMessages = new ArrayList<MessageResponse>();

        addIntialContactPosterMessage(conv, role, transformedMessages, filtered.get(0));
        addReplyMessages(role, conv, filtered, transformedMessages);

        if (transformedMessages.size() == 0) {
            return Optional.absent();
        } else {
            return Optional.of(transformedMessages);
        }
    }

    private List<Message> filterMessages(List<Message> messageRts, Conversation conv, String email, ConversationRole role, boolean robotEnabled) {
        List<Message> filtered = new ArrayList<Message>();
        for (Message item : messageRts) {
            if (item.getState() != MessageState.IGNORED && shouldBeIncluded(item, conv, email, role, robotEnabled)) {
                filtered.add(item);
            }
        }


        return filtered;
    }


    private void addIntialContactPosterMessage(Conversation conv, ConversationRole role, List<MessageResponse> transformedMessages, Message firstMessage) {
        String phoneNumber = conv.getCustomValues().get(BUYER_PHONE_FIELD);

        transformedMessages.add(
                new MessageResponse(
                    MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(firstMessage.getReceivedAt()),
                    Optional.fromNullable(MessageType.getOffer(firstMessage)),
                    Optional.fromNullable(MessageType.getRobot(firstMessage)),
                    ConversationBoundnessFinder.boundnessForRole(role, firstMessage.getMessageDirection()),
                    differ.cleanupFirstMessage(firstMessage.getPlainTextBody()),
                    Optional.fromNullable(phoneNumber),
                    MessageResponse.Attachment.transform(firstMessage),
                    conv.getBuyerId(),
                    MessageType.getLinks(firstMessage)));
    }


    private void addReplyMessages(ConversationRole role, Conversation conv, List<Message> messageRts, List<MessageResponse> transformedMessages) {

        // start with '1' we handled first message with cleanup above
        for (int i = 1; i < messageRts.size(); i++) {

            String diffedMessage;

            if (contactPosterForExistingConversation(messageRts.get(i))) {
                // new contactPoster from payload point of view same as first message of a conversation
                diffedMessage = differ.cleanupFirstMessage(messageRts.get(i).getPlainTextBody());
            } else if (comesFromMessageBoxClient(messageRts.get(i))) {
                // no need to strip or diff anything, in message-box the whole payload is a additional message
                diffedMessage = messageRts.get(i).getPlainTextBody().trim();
            } else {
                diffedMessage = differ.cleanupFirstMessage(messageRts.get(i).getPlainTextBody().trim());
            }


            transformedMessages.add(
                    new MessageResponse(
                        MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(messageRts.get(i).getReceivedAt()),
                        Optional.fromNullable(MessageType.getOffer(messageRts.get(i))),
                        Optional.fromNullable(MessageType.getRobot(messageRts.get(i))),
                        ConversationBoundnessFinder.boundnessForRole(role, messageRts.get(i).getMessageDirection()),
                        diffedMessage,
                        Optional.<String>absent(),
                        MessageResponse.Attachment.transform(messageRts.get(i)),
                        messageRts.get(i).getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? conv.getBuyerId() : conv.getSellerId(),
                        MessageType.getLinks(messageRts.get(i))
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

    private boolean shouldBeIncluded(Message messageRts, Conversation conversationRts, String email, ConversationRole role, boolean robotEnabled) {
        return (messageRts.getState() == MessageState.SENT || isOwnMessage(email, conversationRts, messageRts)) && includeRobotMessage(messageRts, role, robotEnabled);
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

    private boolean includeRobotMessage(Message messageRts, ConversationRole role, boolean robotEnabled) {
        if (robotEnabled) {
            if (MessageType.isRobot(messageRts) && messageRts.getMessageDirection().equals(MessageDirection.SELLER_TO_BUYER) && role.equals(ConversationRole.Seller)) {
                return false;
            }
            if (MessageType.isRobot(messageRts) && messageRts.getMessageDirection().equals(MessageDirection.BUYER_TO_SELLER) && role.equals(ConversationRole.Buyer)) {
                return false;
            }
        } else {
            if (MessageType.isRobot(messageRts)) {
                return false;
            }
        }
        return true;
    }

    private boolean comesFromMessageBoxClient(Message messageRts) {
        return messageRts.getHeaders().containsKey(REPLY_CHANNEL) &&
                (messageRts.getHeaders().get(REPLY_CHANNEL).contains("api") ||
                        messageRts.getHeaders().get(REPLY_CHANNEL).contains("desktop"));
    }

    private boolean contactPosterForExistingConversation(Message messageRts) {
        return messageRts.getHeaders().containsKey(REPLY_CHANNEL) &&
                messageRts.getHeaders().get(REPLY_CHANNEL).startsWith("cp_");
    }

}
