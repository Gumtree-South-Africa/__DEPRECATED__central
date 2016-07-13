package com.ecg.messagecenter.util;

import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
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

import static com.ecg.messagecenter.util.EmailHeaderFolder.unfold;

public class MessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesResponseFactory.class);

    private static final Histogram TEXT_SIZE_CONVERSATIONS = TimingReports.newHistogram("message-box.text-size-conversations");

    static final String BUYER_PHONE_FIELD = "buyer-phonenumber";

    private final UserIdentifierService userIdentifierService;

    public MessagesResponseFactory(UserIdentifierService userIdentifierService) {
        this.userIdentifierService = userIdentifierService;
    }

    public Optional<MessageResponse> latestMessage(String id, Conversation conv) {

        List<Message> filtered = filterMessages(conv.getMessages(), conv, id);

        if (filtered.size() == 0) {
            return Optional.empty();
        }

        ConversationRole role = userIdentifierService.getRoleFromConversation(id, conv);

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
            return Optional.empty();
        } else {
            return Optional.of(Iterables.getLast(transformedMessages));
        }
    }

    public Optional<List<MessageResponse>> create(String id, Conversation conv, List<Message> messageRts) {
        List<Message> filtered = filterMessages(messageRts, conv, id);

        if (filtered.size() == 0) {
            return Optional.empty();
        }

        ConversationRole role = userIdentifierService.getRoleFromConversation(id, conv);

        List<MessageResponse> transformedMessages = new ArrayList<>();

        addIntialContactPosterMessage(conv, role, transformedMessages, filtered.get(0));
        addReplyMessages(role, conv, filtered, transformedMessages);

        if (transformedMessages.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(transformedMessages);
        }
    }

    public List<Message> filterMessages(List<Message> messageRts, Conversation conv, String id) {
        List<Message> filtered = new ArrayList<>();
        for (Message item : messageRts) {
            if (item.getState() != MessageState.IGNORED && shouldBeIncluded(item, conv, id)) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    public String getCleanedMessage(Conversation conv, Message message) {
        if (contactPosterForExistingConversation(message) || comesFromMessageBoxClient(message)) {
            return getUserMessage(message);
        } else {
            return MessagePreProcessor.removeEmailClientReplyFragment(conv, message);
        }
    }

    private void addIntialContactPosterMessage(Conversation conv, ConversationRole role, List<MessageResponse> transformedMessages, Message firstMessage) {
        String phoneNumber = conv.getCustomValues().get(BUYER_PHONE_FIELD);

        transformedMessages.add(
                new MessageResponse(
                        MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(firstMessage.getReceivedAt()),
                        firstMessage.getHeaders().get("X-Offerid"),
                        ConversationBoundnessFinder.boundnessForRole(role, firstMessage.getMessageDirection()),
                        getUserMessage(firstMessage),
                        Optional.ofNullable(phoneNumber),
                        MessageResponse.Attachment.transform(firstMessage),
                        conv.getBuyerId()));
    }

    private void addReplyMessages(ConversationRole role, Conversation conv, List<Message> messageRts, List<MessageResponse> transformedMessages) {
        // start with '1' we handled first message with cleanup above
        for (int i = 1; i < messageRts.size(); i++) {
            transformedMessages.add(
                    new MessageResponse(
                            MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(messageRts.get(i).getReceivedAt()),
                            messageRts.get(i).getHeaders().get("X-Offerid"),
                            ConversationBoundnessFinder.boundnessForRole(role, messageRts.get(i).getMessageDirection()),
                            getCleanedMessage(conv, messageRts.get(i)),
                            Optional.<String>empty(),
                            MessageResponse.Attachment.transform(messageRts.get(i)),
                            messageRts.get(i).getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? conv.getBuyerId() : conv.getSellerId()
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

    private boolean shouldBeIncluded(Message messageRts, Conversation conversationRts, String id) {
        return messageRts.getState() == MessageState.SENT || isOwnMessage(id, conversationRts, messageRts);
    }

    private boolean isOwnMessage(String id, Conversation conversationRts, Message messageRts) {
        if (messageRts.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
            Optional<String> buyerUserId = userIdentifierService.getBuyerUserId(conversationRts);
            if (buyerUserId.isPresent() && buyerUserId.get().equals(id)) {
                return true;
            }
        }
        if (messageRts.getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
            Optional<String> sellerUserId = userIdentifierService.getSellerUserId(conversationRts);
            if (sellerUserId.isPresent() && sellerUserId.get().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String getUserMessage(Message message) {
        String userMessageFromHeader ="";
        String userMessage = "";
        if((userMessageFromHeader = message.getHeaders().get("X-Contact-Poster-User-Message")) != null){
            userMessage = unfold(userMessageFromHeader);
        }else if((userMessageFromHeader = message.getHeaders().get("X-User-Message"))!= null){
            userMessage = unfold(userMessageFromHeader);
        }else{
            userMessage = message.getPlainTextBody();
        }
        return userMessage.trim();
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
