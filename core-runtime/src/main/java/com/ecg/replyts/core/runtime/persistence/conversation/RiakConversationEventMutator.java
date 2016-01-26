package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.cap.Mutation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Appends a event to the list of existing events.
 */
class RiakConversationEventMutator implements Mutation<ConversationEvents> {

    private final List<ConversationEvent> appendEvents;

    public RiakConversationEventMutator(List<ConversationEvent> appendEvents) {
        this.appendEvents = appendEvents;
    }

    @Override
    public ConversationEvents apply(ConversationEvents original) {
        if (original == null || original.getEvents() == null || original.getEvents().isEmpty()) {
            return new ConversationEvents(appendEvents);
        } else {
            return new ConversationEvents(ImmutableList.<ConversationEvent>builder().
                    addAll(original.getEvents()).
                    addAll(appendEvents).
                    build());
        }
    }

}
