package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageModeratedEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HybridConversationRepositoryTest {
    private static final boolean NO_DEEP_MIGRATION = false;

    @Mock
    private DefaultCassandraConversationRepository cassandraRepository;
    @Mock
    private RiakConversationRepository riakRepository;
    @Mock
    private HybridMigrationClusterState migrationState;

    private HybridConversationRepository repository;

    private static final String conversationId = "123";
    private final ConversationCreatedEvent createdEvent = new ConversationCreatedEvent(conversationId, null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
    private final MessageAddedEvent m1 = new MessageAddedEvent("m1", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.SENT, null, null, FilterResultState.OK, ModerationResultState.GOOD, Collections.emptyMap(), null, null, null);
    private final MessageAddedEvent m2 = new MessageAddedEvent("m2", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.SENT, null, null, FilterResultState.OK, ModerationResultState.GOOD, Collections.emptyMap(), null, null, null);


    @Before
    public void setup() {
        repository = new HybridConversationRepository(cassandraRepository, riakRepository, migrationState, NO_DEEP_MIGRATION);
    }

    @Test
    public void testGetIdWithDeepComparisonRiakHasMore() {
        when(riakRepository.getConversationEvents(eq(conversationId))).thenReturn(new ArrayList<>(Arrays.asList(createdEvent, m1, m2)));
        when(cassandraRepository.getConversationEvents(eq(conversationId))).thenReturn(Arrays.asList(createdEvent, m1));

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        assertNotNull(repository.getByIdWithDeepComparison(conversationId));

        verify(cassandraRepository).commit(eq(conversationId), eq(Arrays.asList(m2)));
        verify(riakRepository, never()).commit(anyString(), anyList());
    }

    @Test
    public void testGetIdWithDeepComparisonCassandraHasMore() {
        when(riakRepository.getConversationEvents(eq(conversationId))).thenReturn(new ArrayList<>(Arrays.asList(createdEvent, m1)));
        when(cassandraRepository.getConversationEvents(eq(conversationId))).thenReturn(Arrays.asList(createdEvent, m1, m2));

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        repository.getByIdWithDeepComparison(conversationId);

        verify(cassandraRepository, never()).commit(anyString(), anyListOf(ConversationEvent.class));
        verify(riakRepository, never()).commit(anyString(), anyListOf(ConversationEvent.class));
    }

    @Test
    public void testGetIdWithDeepComparisonEqualConversationEventsInRiakAndCassandra() {
        List<ConversationEvent> conversationEvents = new ArrayList<>(Arrays.asList(createdEvent, m1, m2));

        when(riakRepository.getConversationEvents(eq(conversationId))).thenReturn(conversationEvents);
        when(cassandraRepository.getConversationEvents(eq(conversationId))).thenReturn(conversationEvents);

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        assertNotNull(repository.getByIdWithDeepComparison(conversationId));

        verify(cassandraRepository, never()).commit(anyString(), anyListOf(ConversationEvent.class));
        verify(riakRepository, never()).commit(anyString(), anyListOf(ConversationEvent.class));
    }

    @Test
    public void testGetByIdMigration() {
        ConversationCreatedEvent createdEvent = new ConversationCreatedEvent("123", null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
        MutableConversation conversation = new DefaultMutableConversation(ImmutableConversation.replay(Arrays.asList(createdEvent)));

        when(cassandraRepository.getById(eq("123"))).thenReturn(null);
        when(riakRepository.getById(eq("123"))).thenReturn(conversation);
        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        when(riakRepository.getConversationEvents(eq("123"))).thenReturn(Arrays.asList(createdEvent));

        assertEquals("Mocked conversation should be returned by getById call after Cassandra migration", conversation, repository.getById("123"));

        verify(cassandraRepository).commit(eq("123"), eq(Arrays.asList(createdEvent)));
    }

    @Test
    public void testCommitUnmigratedExisting() {
        ConversationEvent existingConversationEvent = new ConversationCreatedEvent("123", null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
        ConversationEvent newConversationEvent = new MessageAddedEvent("456", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);

        when(cassandraRepository.getLastModifiedDate(eq("123"))).thenReturn(null);
        when(riakRepository.getConversationEvents(eq("123"))).thenReturn(Arrays.asList(existingConversationEvent));
        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        List<ConversationEvent> expectedEventsForRiakCommit = Arrays.asList(newConversationEvent);
        List<ConversationEvent> expectedEventsForCassandraCommit = Arrays.asList(existingConversationEvent, newConversationEvent);

        // Following a commit to a non-migrated conversation, call to Cassandra should contain both original and newly-committed events

        repository.commit("123", Arrays.asList(newConversationEvent));

        verify(riakRepository).commit(eq("123"), eq(expectedEventsForRiakCommit));
        verify(cassandraRepository).commit(eq("123"), eq(expectedEventsForCassandraCommit));
    }

    @Test
    public void testCommitAlreadyMigrated() {
        ConversationEvent newConversationEvent = new MessageAddedEvent("456", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);

        when(cassandraRepository.getLastModifiedDate(eq("123"))).thenReturn(DateTime.now());

        List<ConversationEvent> expectedEventsForBoth = Arrays.asList(newConversationEvent);

        // Following a commit to a previously migrated conversation, call to both should contain just the newly added events

        repository.commit("123", Arrays.asList(newConversationEvent));

        verify(riakRepository).commit(eq("123"), eq(expectedEventsForBoth));
        verify(cassandraRepository).commit(eq("123"), eq(expectedEventsForBoth));
    }

    @Test
    public void testCommitNonexisting() {
        ConversationEvent newConversationEvent = new ConversationCreatedEvent("123", null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());

        when(cassandraRepository.getLastModifiedDate(eq("123"))).thenReturn(null);
        when(riakRepository.getConversationEvents(eq("123"))).thenReturn(null);
        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        List<ConversationEvent> expectedEventsForBoth = Arrays.asList(newConversationEvent);

        // Following a commit to a non-existing conversation, call to both should contain just the newly added events

        repository.commit("123", Arrays.asList(newConversationEvent));

        verify(riakRepository).commit(eq("123"), eq(expectedEventsForBoth));
        verify(cassandraRepository).commit(eq("123"), eq(expectedEventsForBoth));
    }

    @Test
    public void testFixEventOrder() {
        ConversationEvent newConversationCreatedEvent = new ConversationCreatedEvent("123", null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
        ConversationEvent newConvEvent1 = new MessageAddedEvent("456", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);
        ConversationEvent newConvEvent2 = new MessageAddedEvent("789", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);
        ConversationEvent newConvEvent3 = new MessageModeratedEvent("999", DateTime.now(), ModerationResultState.GOOD, null);

        // Test no changes (events were in the correct order)
        List<ConversationEvent> events = Arrays.asList(newConversationCreatedEvent, newConvEvent1, newConvEvent2, newConvEvent3);
        List<ConversationEvent> eventsbeforeFix = events;
        events = repository.fixEventOrder(events);
        assertEquals(eventsbeforeFix, events);
        verifyEventOrder(events);

        // Out of order
        events = Arrays.asList(newConversationCreatedEvent,newConvEvent2, newConvEvent1, newConvEvent3);
        events = repository.fixEventOrder(events);
        verifyEventOrder(events);

        // Out of order with ConversationCreatedEvent out of place
        events = Arrays.asList(newConvEvent2, newConversationCreatedEvent, newConvEvent1, newConvEvent3);
        events = repository.fixEventOrder(events);
        verifyEventOrder(events);

        // Out of order with no ConversationCreatedEvent
        events = Arrays.asList(newConvEvent2, newConversationCreatedEvent, newConvEvent1, newConvEvent3);
        events = repository.fixEventOrder(events);
        verifyEventOrder(events);

        // CreateConversationEvent with later timestamp
        newConversationCreatedEvent = new ConversationCreatedEvent("123", null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());

        events = Arrays.asList(newConversationCreatedEvent, newConvEvent1, newConvEvent2, newConvEvent3);
        events = repository.fixEventOrder(events);
        verifyEventOrder(events);
    }

    private void verifyEventOrder(List<ConversationEvent> events) {
        ConversationEvent lastce  = null;
        for(ConversationEvent e: events) {
            if(lastce==null) {
                lastce = e;
                continue;
            }
            if(lastce.getEventTimeUUID().timestamp()>= e.getEventTimeUUID().timestamp()) {
                fail(String.format("Events out of order! %s/%d and next %s/%d", lastce.getEventId(), lastce.getEventTimeUUID().timestamp(), e.getEventId(), e.getEventTimeUUID().timestamp()));
            }
            lastce = e;
        }
    }
}