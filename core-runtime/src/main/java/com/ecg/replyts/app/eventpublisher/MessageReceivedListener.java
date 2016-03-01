package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ExtendedConversationEvent;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.app.eventpublisher.EventPublisher.Event;

import java.util.List;
import java.util.stream.Collectors;

public class MessageReceivedListener implements ConversationEventListener {

    private final EventPublisher eventPublisher;
    private final MailCloakingService mailCloakingService;
    private final ExtendedConversationEventSerializer serializer;

    public MessageReceivedListener(MailCloakingService mailCloakingService, EventPublisher eventPublisher) {
        this(mailCloakingService, eventPublisher, new ExtendedConversationEventSerializer());
    }

    public MessageReceivedListener(MailCloakingService mailCloakingService, EventPublisher eventPublisher, ExtendedConversationEventSerializer serializer) {
        this.mailCloakingService = mailCloakingService;
        this.eventPublisher = eventPublisher;
        this.serializer = serializer;
    }

    @Override
    public void eventsTriggered(Conversation conversation, List<ConversationEvent> conversationEvents) {
        List<Event> events = getEventsAsJson(conversation, conversationEvents);
        eventPublisher.publishEvents(events);
    }

    private List<Event> getEventsAsJson(Conversation conversation, List<ConversationEvent> conversationEvents) {
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
}
