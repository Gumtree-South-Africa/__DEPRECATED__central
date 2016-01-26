package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link RiakConversationEventMutator}.
 */
public class RiakConversationEventMutatorTest {

    private Map<Integer, MockConversationEvent> commands = new HashMap<>();

    private RiakConversationEventMutator mutator =
            new RiakConversationEventMutator(createEvents(10, 11).getEvents());

    @Test
    public void appendToEmptyList() throws Exception {
        assertThat(mutator.apply(createEvents()), is(createEvents(10, 11)));
    }

    @Test
    public void appendToNullList() throws Exception {
        assertThat(mutator.apply(null), is(createEvents(10, 11)));
    }

    @Test
    public void appendToNonEmptyList() throws Exception {
        assertThat(mutator.apply(createEvents(1, 2)), is(createEvents(1, 2, 10, 11)));
    }

    private ConversationEvents createEvents(int... ids) {
        List<ConversationEvent> resultEvents = new ArrayList<>(ids.length);
        for (int id : ids) {
            resultEvents.add(createEvent(id));
        }
        return new ConversationEvents(resultEvents);
    }

    private ConversationEvent createEvent(int id) {
        MockConversationEvent event = commands.get(id);
        if (event == null) {
            event = new MockConversationEvent(id);
            commands.put(id, event);
        }
        return event;
    }

    private static class MockConversationEvent extends ConversationEvent {
        private MockConversationEvent(int id) {
            super(String.valueOf(id), new DateTime((long) id));
        }

        @Override
        public String toString() {
            return getEventId();
        }
    }
}
