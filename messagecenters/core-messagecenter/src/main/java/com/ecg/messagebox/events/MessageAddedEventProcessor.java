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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

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

    public MessageAddedEventProcessor(EventConverter eventConverter,
                                      MessageAddedKafkaPublisher publisher, boolean enabled) {
        this.enabled = enabled;
        this.eventConverter = eventConverter;
        this.publisher = publisher;
    }

    public void publishMessageAddedEvent(Conversation conv, Message msg, String msgText, UserUnreadCounts unreadCounts) {
        if (enabled) {
            publisher.publishNewMessage(constructEvent(conv, msg, msgText, unreadCounts));
        }
    }

    private EventPublisher.Event constructEvent(Conversation conv, Message msg, String msgText, UserUnreadCounts unreadCounts) {
        final Map<String, String> headers = new HashMap<>(msg.getHeaders());
        headers.put(USER_MESSAGE_HEADER, msgText);

        final MessageAddedEvent messageAddedEvent = new MessageAddedEvent(msg.getId(),
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

        return eventConverter.toEvents(conv, newArrayList(messageAddedEvent), unreadCounts, isNewConnection(conv.getMessages())).get(0);
    }

    @Autowired(required = false)
    public void setPublisher(@Qualifier("messageAddedEventPublisher") MessageAddedKafkaPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Returns true only if the last message creates a connection in the conversation to avoid duplicate connections.
     */
    protected boolean isNewConnection(List<Message> messages) {
        if (messages.size() > 2) {
            final List<Message> messagesNoLatest = new ArrayList<>(messages);
            messagesNoLatest.remove(messagesNoLatest.size() - 1);

            return isConnected(messages) && !isConnected(messagesNoLatest);
        } else {
            return false;
        }
    }

    /**
     * Returns true if conversation has a new connection
     */
    private boolean isConnected(List<Message> messages) {
        final Stack<MessageDirection> stack = new Stack<>();
        messages.forEach(m -> {
            final MessageDirection direction = m.getMessageDirection();
            if (stack.isEmpty()) {
                stack.push(direction);
            } else {
                if (stack.peek() != direction) {
                    stack.push(direction);
                }
            }
        });
        return stack.size() > 2
                && stack.get(0) == MessageDirection.BUYER_TO_SELLER
                && stack.get(1) == MessageDirection.SELLER_TO_BUYER
                && stack.get(2) == MessageDirection.BUYER_TO_SELLER;
    }
}