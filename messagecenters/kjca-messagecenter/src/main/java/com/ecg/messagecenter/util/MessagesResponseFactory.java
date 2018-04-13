package com.ecg.messagecenter.util;

import com.codahale.metrics.Counter;
import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MessagesResponseFactory {
    private static final Counter EMPTY_MSG_COUNTER = TimingReports.newCounter("message-box.removed-empty-msg");

    private MessagesDiffer differ;
    private TextAnonymizer textAnonymizer;

    public MessagesResponseFactory(MessagesDiffer differ, TextAnonymizer textAnonymizer) {
        this.differ = differ;
        this.textAnonymizer = textAnonymizer;
    }

    public Optional<MessageResponse> latestMessage(String email, Conversation conv) {
        return create(email, conv).reduce((first, second) -> second);
    }

    public Stream<MessageResponse> create(String email, Conversation conv) {
        ConversationRole role = ConversationBoundnessFinder.lookupUsersRole(email, conv);

        return buildMessageResponses(role, conv, email);
    }

    private Stream<MessageResponse> buildMessageResponses(ConversationRole role, Conversation conv, String email) {
        return conv.getMessages()
                .stream()
                .filter(msg -> msg.getState() != MessageState.IGNORED && shouldBeIncluded(msg, conv, email))
                .map(msg -> buildSingleMessageResponse(conv, role, msg))
                .filter(Objects::nonNull);
    }

    private MessageResponse buildSingleMessageResponse(Conversation conv, ConversationRole role, Message message) {
        String cleanedUpText = differ.cleanupFirstMessage(message.getPlainTextBody());

        if (StringUtils.isBlank(cleanedUpText)) {
            EMPTY_MSG_COUNTER.inc();
            return null;
        }

        String anonymizedText = textAnonymizer.anonymizeText(conv, cleanedUpText);
        return new MessageResponse(
                message.getId(),
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedAt()),
                ConversationBoundnessFinder.boundnessForRole(role, message.getMessageDirection()),
                anonymizedText,
                Optional.empty(),
                MessageResponse.Attachment.transform(message),
                message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? conv.getBuyerId() : conv.getSellerId()
        );
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
}
