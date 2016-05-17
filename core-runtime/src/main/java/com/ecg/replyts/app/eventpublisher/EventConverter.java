package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ExtendedConversationEvent;
import com.ecg.replyts.app.eventpublisher.EventPublisher.Event;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts conversation events to publishable events.
 */
public class EventConverter {

    private MailCloakingService mailCloakingService;

    private final ExtendedConversationEventSerializer serializer;

    public EventConverter(MailCloakingService mailCloakingService) {
        this(mailCloakingService, new ExtendedConversationEventSerializer());
    }

    public EventConverter(MailCloakingService mailCloakingService, ExtendedConversationEventSerializer serializer) {
        this.mailCloakingService = mailCloakingService;
        this.serializer = serializer;
    }

    public List<Event> toEvents(Conversation conversation, List<ConversationEvent> conversationEvents) {
        String sellerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation).getAddress();
        String buyerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation).getAddress();

        return conversationEvents
                .stream()
                .map(conversationEvent -> {
                    String partitionKey = conversation.getId();
                    ExtendedConversationEvent extendedConversationEvent =
                            new ExtendedConversationEvent(conversation, conversationEvent, sellerAnonymousEmail, buyerAnonymousEmail);
                    byte[] data = serializer.serialize(extendedConversationEvent);
                    return new Event(partitionKey, data);
                })
                .collect(Collectors.<Event>toList());
    }

    public List<Event> toEvents(List<ImmutablePair<Conversation, ConversationEvent>> pairs) {
        List<Event> events = new ArrayList<>(pairs.size());
        for (ImmutablePair<Conversation, ConversationEvent> pair : pairs) {
            events.addAll(toEvents(pair.getLeft(), Collections.singletonList(pair.getRight())));
        }
        return events;
    }
}