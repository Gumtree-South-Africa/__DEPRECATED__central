package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.app.eventpublisher.EventPublisher.Event;

import java.util.List;

public class MessageReceivedListener implements ConversationEventListener {

    private final EventConverter eventConverter;
    private final EventPublisher eventPublisher;

    public MessageReceivedListener(EventConverter eventConverter, EventPublisher eventPublisher) {
        this.eventConverter = eventConverter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void eventsTriggered(Conversation conversation, List<ConversationEvent> conversationEvents) {
        List<Event> events = eventConverter.toEvents(conversation, conversationEvents);
        eventPublisher.publishConversationEvents(events);
    }
}