package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A very simple pojo wrapper for a list of {@link ConversationEvent}s.
 */
public class ConversationEvents {
    private final List<ConversationEvent> events;
    private final int siblingCount;

    public ConversationEvents(List<ConversationEvent> events) {
        this(events, 0);
    }

    public ConversationEvents(List<ConversationEvent> events, int siblingCount) {
        this.siblingCount = siblingCount;
        this.events = events;
    }

    public List<ConversationEvent> getEvents() {
        return events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationEvents that = (ConversationEvents) o;
        return events.equals(that.events);
    }

    @Override
    public int hashCode() {
        return events.hashCode();
    }

    /**
     * returns the conversation created event. this is by convention (and by common sense) the first event in the
     * list of conversation events.
     */
    public ConversationCreatedEvent createdEvent() {
        return (ConversationCreatedEvent) events.get(0);
    }

    /**
     * returns a mutable copy of all follow up events. These are all events that are not the conversation created event.
     */
    public List<ConversationEvent> followUpEvents() {
        return Lists.newArrayList(events.subList(1, events.size()));
    }

    public boolean needsMerging() {
        return siblingCount > 1;
    }
}