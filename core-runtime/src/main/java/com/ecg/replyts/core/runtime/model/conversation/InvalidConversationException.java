package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;

import java.util.List;

/**
 * Conversation that cannot be replayed because it lacks ConversationCreatedEvent
 */
public class InvalidConversationException extends IllegalStateException {

    private List<ConversationEvent> events;

    public InvalidConversationException(List<ConversationEvent> events) {
        super();
        this.events = events;
    }

    @Override
    public String toString() {
        return String.format("Did not find ConversationCreatedEvent among the list of %d events. %s", events.size(), super.toString());
    }

    public List<ConversationEvent> getEvents() {
        return events;
    }
}
