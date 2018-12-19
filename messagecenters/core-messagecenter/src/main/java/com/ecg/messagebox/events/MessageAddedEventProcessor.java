package com.ecg.messagebox.events;

import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@Component
public class MessageAddedEventProcessor {

    @Value("${replyts.message-added-events.publisher.enabled:false}")
    private boolean enabled;
    private MessageAddedKafkaPublisher publisher;
    private EventConverter eventConverter;
    private static final String USER_MESSAGE_HEADER = "X-User-Message";

    @Autowired
    public MessageAddedEventProcessor(MailCloakingService mailCloaking) {
        this.eventConverter = new EventConverter(mailCloaking);
    }

    public MessageAddedEventProcessor(EventConverter eventConverter, MessageAddedKafkaPublisher publisher, boolean enabled) {
        this.enabled = enabled;
        this.eventConverter = eventConverter;
        this.publisher = publisher;
    }

    public void publishMessageAddedEvent(Conversation conv, Message msg, String msgText, UserUnreadCounts unreadCounts) {
        if (enabled) {
            publisher.publishNewMessage(constructEvent(conv, msg, msgText, unreadCounts));
        }
    }

    public void publishMessageAddedEvent(Conversation conv, String id, String msgText, UserUnreadCounts unreadCounts) {
        if (enabled) {
            publisher.publishNewMessage(constructEvent(conv, id, msgText, unreadCounts));
        }
    }

    private EventPublisher.Event constructEvent(Conversation conv, Message msg, String msgText, UserUnreadCounts unreadCounts) {
        Map<String, String> headers = new HashMap<>(msg.getCaseInsensitiveHeaders());
        headers.put(USER_MESSAGE_HEADER, msgText);

        final MessageAddedEvent messageAddedEvent = new MessageAddedEvent(
                msg.getId(),
                msg.getMessageDirection(),
                msg.getReceivedAt(),
                msg.getState(),
                msg.getSenderMessageIdHeader(),
                msg.getInResponseToMessageId(),
                FilterResultState.OK,
                ModerationResultState.GOOD,
                headers,
                msgText,
                msg.getAttachmentFilenames(),
                singletonList(msgText));

        return eventConverter.toEvents(conv, singletonList(messageAddedEvent), unreadCounts, isNewConnection(conv.getMessages())).get(0);
    }

    private EventPublisher.Event constructEvent(Conversation conv, String id, String msgText, UserUnreadCounts unreadCounts) {
        Map<String, String> headers = singletonMap(USER_MESSAGE_HEADER, msgText);

        final MessageAddedEvent messageAddedEvent = new MessageAddedEvent(id,
                MessageDirection.SYSTEM_MESSAGE,
                DateTime.now(),
                null,
                null,
                null,
                FilterResultState.OK,
                ModerationResultState.GOOD,
                headers,
                msgText,
                null,
                singletonList(msgText));

        return eventConverter.toEvents(conv, singletonList(messageAddedEvent), unreadCounts, isNewConnection(conv.getMessages())).get(0);
    }

    @Autowired(required = false)
    public void setPublisher(@Qualifier("messageAddedEventPublisher") MessageAddedKafkaPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Returns true only if the last message creates a connection in the conversation to avoid duplicate connections.
     */
    protected boolean isNewConnection(List<Message> messages) {
        return messages.size() > 2 && isConnected(messages) && !isConnected(messages.subList(0, messages.size() - 1));
    }

    /**
     * Returns true if conversation has a new connection
     */
    private boolean isConnected(List<Message> messages) {
        int i = 0;
        for (Message message : messages) {
            MessageDirection direction = message.getMessageDirection();
            if (i == 0 && direction == BUYER_TO_SELLER) {
                i++;
            } else if (i == 1 && direction == SELLER_TO_BUYER) {
                i++;
            } else if (i == 2 && direction == BUYER_TO_SELLER) {
                return true;
            }
        }
        return false;
    }
}