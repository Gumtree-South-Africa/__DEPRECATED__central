package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link RiakConversationEventConflictResolver}.
 */
public class RiakConversationEventConflictResolverTest {

    private RiakConversationEventConflictResolver resolver;

    private static final ConversationCreatedEvent INITIAL_EVENT = new ConversationCreatedEvent("convid", "adid", "buyerid", "sellerid", "bs", "ss", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap());

    @Before
    public void setUp() throws Exception {
        resolver = new RiakConversationEventConflictResolver();
    }

    @Test
    public void resolveNullToNull() throws Exception {
        assertThat(resolver.resolve(null), is(nullValue()));
    }

    @Test
    public void resolveEmptyListToNull() throws Exception {
        List<ConversationEvents> events = Collections.emptyList();
        assertThat(resolver.resolve(events), is(nullValue()));
    }

    @Test
    public void resolveSingletonListToOnlyValue() throws Exception {
        ConversationEvents events = createEvents(1);
        assertThat(resolver.resolve(Collections.singletonList(events)), is(events));
    }

    @Test
    public void resolveTwoSameListToOnlyValue() throws Exception {
        ConversationEvents events1 = createEvents(1, 2, 3);
        ConversationEvents events2 = createEvents(1, 2, 3);
        assertThat(resolver.resolve(ImmutableList.of(events1, events2)), is(events1));
    }

    @Test
    public void resolveThreeSameListToOnlyValue() throws Exception {
        ConversationEvents events1 = createEvents(1, 2, 3);
        ConversationEvents events2 = createEvents(1, 2, 3);
        ConversationEvents events3 = createEvents(1, 2, 3);
        assertThat(resolver.resolve(ImmutableList.of(events1, events2, events3)), is(events1));
    }

    @Test
    public void resolveTwoSameListWithFirstShorterToLongestMatch() throws Exception {
        ConversationEvents events1 = createEvents(1, 2, 3);
        ConversationEvents events2 = createEvents(1, 2, 3, 4, 5);
        assertThat(resolver.resolve(ImmutableList.of(events1, events2)), is(events2));
    }

    @Test
    public void resolveTwoSameListWithFirstLongerToLongestMatch() throws Exception {
        ConversationEvents events1 = createEvents(1, 2, 3, 4, 5);
        ConversationEvents events2 = createEvents(1, 2, 3);
        assertThat(resolver.resolve(ImmutableList.of(events1, events2)), is(events1));
    }

    @Test
    public void resolveTwoSameListWithDifferentHistories() throws Exception {
        ConversationEvents events1 = createEvents(1, 2, 3, 6, 7, 8);
        ConversationEvents events2 = createEvents(1, 2, 3, 4, 5, 8);
        ConversationEvents merged = createEvents(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(resolver.resolve(ImmutableList.of(events1, events2)), is(merged));
    }

    @Test
    public void resolveThreeListWithDifferentHistories() throws Exception {
        ConversationEvents events1 = createEvents(1, 2, 3, 6, 7, 10);
        ConversationEvents events2 = createEvents(1, 2, 3, 4, 5, 10);
        ConversationEvents events3 = createEvents(1, 2, 3, 8, 9, 10);
        ConversationEvents merged = createEvents(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(resolver.resolve(ImmutableList.of(events3, events2, events1)), is(merged));
    }


    @Test
    public void joinRecreatedConversationWithExcistingConversation() throws Exception {
        // Support recovery after dual datacenter connection loss
        DateTime originalStartedDate = DateTime.now().minusDays(2);
        ConversationEvents left = new ConversationEvents(Arrays.<ConversationEvent>asList(
                new ConversationCreatedEvent(
                        "convid1",
                        "123",
                        "buyer@host.com",
                        "seller@host.com",
                        "buyersecret",
                        "sellersecret",
                        originalStartedDate,
                        ConversationState.ACTIVE,
                        Maps.<String, String>newHashMap()),

                new MessageAddedEvent(
                        "msg1",
                        MessageDirection.BUYER_TO_SELLER,
                        originalStartedDate,
                        MessageState.SENT,
                        "<hosd89puawh4hhwef@platform.com>",
                        null,
                        FilterResultState.OK,
                        ModerationResultState.UNCHECKED,
                        Maps.<String, String>newHashMap(),
                        "foobar", Collections.<String>emptyList()
                )
        ));

        ConversationEvents right = new ConversationEvents(Arrays.<ConversationEvent>asList(
                new ConversationCreatedEvent(
                        "convid1",
                        "123",
                        "buyer@host.com",
                        "seller@host.com",
                        "buyersecret",
                        "sellersecret",
                        DateTime.now().minusDays(1),
                        ConversationState.ACTIVE,
                        Maps.<String, String>newHashMap()),
                new MessageAddedEvent(
                        "msg2",
                        MessageDirection.BUYER_TO_SELLER,
                        originalStartedDate,
                        MessageState.SENT,
                        "<09pjel5k4hgpoainjbfs@platform.com>",
                        null,
                        FilterResultState.OK,
                        ModerationResultState.UNCHECKED,
                        Maps.<String, String>newHashMap(),
                        "foobar", Collections.<String>emptyList()
                )
        ));

        ConversationEvents mergedEvents = resolver.resolve(Lists.newArrayList(left, right));

        List<ConversationEvent> evts = mergedEvents.getEvents();
        assertEquals(3, evts.size());

        // assert that there is only one conversation created event (the first one)
        // and two message added events (first msg1 then msg2, as this is the correct timely order)
        ConversationCreatedEvent e1 = (ConversationCreatedEvent) evts.get(0);
        assertEquals(originalStartedDate, ((ConversationCreatedEvent) (evts.get(0))).getCreatedAt());
        assertEquals("msg1", ((MessageAddedEvent) evts.get(1)).getMessageId());
        assertEquals("msg2", ((MessageAddedEvent) evts.get(2)).getMessageId());
    }

    private ConversationEvents createEvents(int... ids) {
        List<ConversationEvent> resultEvents = new ArrayList<>(ids.length);
        resultEvents.add(INITIAL_EVENT);
        for (int id : ids) {
            resultEvents.add(createEvent(id));
        }
        return new ConversationEvents(resultEvents);
    }

    private ConversationEvent createEvent(int id) {

        MockConversationEvent event = new MockConversationEvent(id);
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
