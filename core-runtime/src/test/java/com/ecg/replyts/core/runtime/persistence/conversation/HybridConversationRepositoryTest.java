package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HybridConversationRepositoryTest {
    @Mock
    private DefaultCassandraConversationRepository cassandraRepository;
    @Mock
    private RiakConversationRepository riakRepository;
    @Mock
    private HybridMigrationClusterState migrationState;

    @InjectMocks
    private HybridConversationRepository repository;

    @Test
    public void testMigrationHappensInBatches() {
        String conversationId = "123aaa";
        ReflectionTestUtils.setField(repository, "maxBatchSizeCassandra", 20);

        List<ConversationEvent> events = new ArrayList<>();
        events.add(new ConversationCreatedEvent(conversationId, null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>()));
        for (int i = 0; i < 65; i++) {
            events.add(new MessageAddedEvent("m" + i, MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null));
        }

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);
        repository.migrateEventsToCassandra(conversationId, events);
        verify(cassandraRepository, times(4)).commit(eq(conversationId), anyListOf(ConversationEvent.class));
    }

    @Test
    public void testGetIdWithDeepComparisonRiakHasMore() {
        String conversationId = "123";
        ConversationCreatedEvent createdEvent = new ConversationCreatedEvent(conversationId, null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
        MessageAddedEvent m1 = new MessageAddedEvent("m1", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);
        MessageAddedEvent m2 = new MessageAddedEvent("m2", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);

        when(riakRepository.getConversationEvents(eq(conversationId))).thenReturn(new ArrayList<>(Arrays.asList(createdEvent, m1, m2)));
        when(cassandraRepository.getConversationEvents(eq(conversationId))).thenReturn(Arrays.asList(createdEvent, m1));

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        assertEquals("ConversationId: 123, more events in Riak than in Cassandra, saving 1 new events", repository.getByIdWithDeepComparison(conversationId));

        verify(cassandraRepository).commit(eq(conversationId), eq(Arrays.asList(m2)));
        verify(riakRepository, never()).commit(anyString(), anyList());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetIdWithDeepComparisonCassandraHasMore() {
        String conversationId = "123";
        ConversationCreatedEvent createdEvent = new ConversationCreatedEvent(conversationId, null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
        MessageAddedEvent m1 = new MessageAddedEvent("m1", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);
        MessageAddedEvent m2 = new MessageAddedEvent("m2", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);

        when(riakRepository.getConversationEvents(eq(conversationId))).thenReturn(new ArrayList<>(Arrays.asList(createdEvent, m1)));
        when(cassandraRepository.getConversationEvents(eq(conversationId))).thenReturn(Arrays.asList(createdEvent, m1, m2));

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        repository.getByIdWithDeepComparison(conversationId);

        verify(cassandraRepository, never()).commit(anyString(), anyListOf(ConversationEvent.class));
        verify(riakRepository, never()).commit(anyString(), anyListOf(ConversationEvent.class));
    }

    @Test
    public void testGetIdWithDeepComparisonEqualConversationEventsInRiakAndCassandra() {
        String conversationId = "123";
        ConversationCreatedEvent createdEvent = new ConversationCreatedEvent(conversationId, null, null, null, null, null, DateTime.now(), ConversationState.ACTIVE, new HashMap<>());
        MessageAddedEvent m1 = new MessageAddedEvent("m1", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);
        MessageAddedEvent m2 = new MessageAddedEvent("m2", MessageDirection.BUYER_TO_SELLER, DateTime.now(), null, null, null, FilterResultState.OK, ModerationResultState.GOOD, null, null, null, null);

        when(riakRepository.getConversationEvents(eq(conversationId))).thenReturn(new ArrayList<>(Arrays.asList(createdEvent, m1, m2)));
        when(cassandraRepository.getConversationEvents(eq(conversationId))).thenReturn(Arrays.asList(createdEvent, m1, m2));

        when(migrationState.tryClaim(any(Class.class), anyString())).thenReturn(true);

        assertEquals("ConversationId: 123, events in Cassandra and Riak are equal", repository.getByIdWithDeepComparison(conversationId));

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
}