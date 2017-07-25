package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.command.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIdx;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class DefaultCassandraConversationRepositoryIntegrationTest extends ConversationRepositoryIntegrationTestBase<DefaultCassandraConversationRepository> {
    private String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private Session session = null;

    private CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();

    @Override
    protected DefaultCassandraConversationRepository createConversationRepository() {
        if (session == null) {
            session = casdb.initStdSchema(KEYSPACE);
        }

        DefaultCassandraConversationRepository myRepo = new DefaultCassandraConversationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
        myRepo.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());

        return myRepo;
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }

    @Test
    public void shouldStoreManyEvents() {
        String conversationId = UUID.randomUUID().toString();

        int nrOfEvents = 100;

        List<ConversationCommand> commands = new ArrayList<>();
        commands.add(newConversationCommand(conversationId));

        for (int i = nrOfEvents; i > 1; i--) {
            DateTime date = now().minusDays(i);
            commands.add(newAddMessageCommand(conversationId, UUID.randomUUID().toString(), date));
        }
        ConversationCommand[] stockArr = new ConversationCommand[commands.size()];
        given(commands.toArray(stockArr));

        List<ConversationEvent> conversationEvents = getConversationRepository().getConversationEvents(conversationId);
        assertEquals(nrOfEvents, conversationEvents.size());
    }

    @Test
    public void conversationEventsCommittedInWrongOrderAreSortedAndUpdateLastModifiedDate() {
        DateTime t = now();
        ConversationEvent conversationCreatedEvent = new ConversationCreatedEvent(conversationId1, "ad1", "b1", "s1", "bs", "ss", t.minusDays(3), ConversationState.ACTIVE, new HashMap<>());
        ConversationEvent messageAddedEvent = new MessageAddedEvent("m1", MessageDirection.BUYER_TO_SELLER, t.minusDays(2), MessageState.SENT, "", "", FilterResultState.OK, ModerationResultState.GOOD, null, "", null, null);
        ConversationEvent messageAddedEventLater = new MessageAddedEvent("m2", MessageDirection.BUYER_TO_SELLER, t.minusDays(1), MessageState.SENT, "", "", FilterResultState.OK, ModerationResultState.GOOD, null, "", null, null);

        conversationRepository.commit(conversationId1, Collections.singletonList(messageAddedEvent));

        assertEquals(t.minusDays(2), new DateTime(getConversationRepository().getLastModifiedDate(conversationId1)));
        assertEquals(1, getConversationRepository().getConversationEvents(conversationId1).size());

        conversationRepository.commit(conversationId1, Arrays.asList(conversationCreatedEvent, messageAddedEventLater));

        assertEquals(t.minusDays(1), new DateTime(getConversationRepository().getLastModifiedDate(conversationId1)));
        List<ConversationEvent> conversationEvents = getConversationRepository().getConversationEvents(conversationId1);
        assertEquals(3, conversationEvents.size());
        assertTrue(conversationEvents.get(0) instanceof ConversationCreatedEvent);
        assertEquals("m1", ((MessageAddedEvent) conversationEvents.get(1)).getMessageId());
        assertEquals("m2", ((MessageAddedEvent) conversationEvents.get(2)).getMessageId());
    }

    @Test
    public void shouldNotStoreSameEventTwice() {
        DateTime timeNow = now();
        DateTime firstMsgReceivedAtDateTime = new DateTime(timeNow.getYear(), timeNow.getMonthOfYear(), timeNow.getDayOfMonth(), 9, 11, 43);

        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));
        List<ConversationEvent> conversationEvents = getConversationRepository().getConversationEvents(conversationId1);
        assertEquals(2, conversationEvents.size());

        conversationRepository.commit(conversationId1, conversationEvents);
        conversationEvents = getConversationRepository().getConversationEvents(conversationId1);
        assertEquals(2, conversationEvents.size());
    }

    @Test
    public void shouldAddNewEvents() {
        DateTime timeNow = now();
        DateTime firstMsgReceivedAtDateTime = new DateTime(timeNow.getYear(), timeNow.getMonthOfYear(), timeNow.getDayOfMonth(), 9, 11, 43);

        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));
        List<ConversationEvent> conversationEvents = getConversationRepository().getConversationEvents(conversationId1);
        assertEquals(2, conversationEvents.size());

        conversationEvents.add(new MessageAddedEvent(newAddMessageCommand(conversationId1, "hoops", DateTime.now())));
        conversationRepository.commit(conversationId1, conversationEvents);

        conversationEvents = getConversationRepository().getConversationEvents(conversationId1);
        assertEquals(3, conversationEvents.size());
    }

    @Test
    public void shouldStoreConversationEventIdxByHour() {
        DateTime timeNow = now();
        DateTime firstMsgReceivedAtDateTime = new DateTime(timeNow.getYear(), timeNow.getMonthOfYear(), timeNow.getDayOfMonth(), 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));
        List<ConversationEventIdx> conversationEvents = getConversationRepository().streamConversationEventIdxsByHour(timeNow)
                .collect(Collectors.toList());
        assertEquals(1, conversationEvents.size());
        ConversationEventIdx conversationEventByHour = conversationEvents.get(0);
        assertEquals(conversationEventByHour.getConversationId(), conversationId1);
    }

    @Test
    public void shouldGetLastModificationIdx() throws InterruptedException {
        // create conversation
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));
        assertEquals(firstMsgReceivedAtDateTime, new DateTime(getConversationRepository().getLastModifiedDate(conversationId1)));

        // update conversation
        DateTime secondMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 12, 22, 5);
        DefaultMutableConversation conversation = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation.applyCommand(newAddMessageCommand(conversationId1, "msg456", secondMsgReceivedAtDateTime));
        conversation.commit(conversationRepository, conversationEventListeners);

        assertEquals(secondMsgReceivedAtDateTime, new DateTime(getConversationRepository().getLastModifiedDate(conversationId1)));
        assertNull(getConversationRepository().getLastModifiedDate(conversationId2));
    }

    @Test
    public void shouldDeleteConversationModificationIdxsForConversation() {
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));

        getConversationRepository().deleteConversationModificationIdxs(conversationId1);

        assertNull(getConversationRepository().getLastModifiedDate(conversationId1));
    }

    @Test
    public void shouldDeleteConversationEventIdx() {
        DateTime firstMsgReceivedAtDateTime = now();
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));

        List<ConversationEventIdx> conversationEventIdsByHour = getConversationRepository().streamConversationEventIdxsByHour(firstMsgReceivedAtDateTime)
                .collect(Collectors.toList());

        getConversationRepository().deleteConversationEventIdx(conversationEventIdsByHour.get(0));

        assertTrue(getConversationRepository().streamConversationEventIdxsByHour(firstMsgReceivedAtDateTime).collect(Collectors.toList()).isEmpty());
    }

    @Test
    public void shouldStreamConversationEventIdxsByHour() {
        UUID eventId1 = UUIDs.timeBased();
        UUID eventId2 = UUIDs.timeBased();

        // create conversation
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        ConversationEventIdx firstConversationEventIdx = new ConversationEventIdx(firstMsgReceivedAtDateTime.hourOfDay().roundFloorCopy(), conversationId1, eventId1);
        conversationRepository.insertConversationEventIdx(firstConversationEventIdx);

        // update conversation
        DateTime secondMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 22, 5);
        ConversationEventIdx secondConversationEventIdx = new ConversationEventIdx(secondMsgReceivedAtDateTime.hourOfDay().roundFloorCopy(), conversationId1, eventId2);
        conversationRepository.insertConversationEventIdx(secondConversationEventIdx);

        List<ConversationEventIdx> conversationModificationIdxs = getConversationRepository().streamConversationEventIdxsByHour(firstMsgReceivedAtDateTime)
                .collect(Collectors.toList());
        assertEquals(2, conversationModificationIdxs.size());
        assertTrue(conversationModificationIdxs.contains(firstConversationEventIdx));
        assertTrue(conversationModificationIdxs.contains(secondConversationEventIdx));
    }

    @Test
    public void findEventsCreatedBetween() throws Exception {
        givenABunchOfCommands();

        Stream<ImmutablePair<Conversation, ConversationEvent>> eventStream = conversationRepository.findEventsCreatedBetween(new DateTime().minus(1000), new DateTime().plus(1000));
        assertThat(eventStream.count(), is(6L));
    }

    @Test
    public void doesntFindEventsCreatedBetween() throws Exception {
        givenABunchOfCommands();

        Stream<ImmutablePair<Conversation, ConversationEvent>> eventStream = conversationRepository.findEventsCreatedBetween(new DateTime().minus(2000), new DateTime().minus(1000));
        assertThat(eventStream.count(), is(0L));
    }

    @Test
    public void deleteConversationCommandDeletesModificationIdxs() {
        // create conversation
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));

        // update conversation
        DateTime secondMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 22, 5);
        DefaultMutableConversation conversation = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation.applyCommand(newAddMessageCommand(conversationId1, "msg456", secondMsgReceivedAtDateTime));
        conversation.commit(conversationRepository, conversationEventListeners);

        conversation.applyCommand(new ConversationDeletedCommand(conversationId1, now()));
        conversation.commit(this.conversationRepository, conversationEventListeners);

        List<ConversationEventIdx> conversationEventIdxs = getConversationRepository().streamConversationEventIdxsByHour(firstMsgReceivedAtDateTime.hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());
        assertTrue(conversationEventIdxs.isEmpty());
        assertNull(getConversationRepository().getLastModifiedDate(conversationId1));
    }

    @Test
    public void deleteConversationCommandDeletesConversationEventsByHourIdxs() {
        // create conversation
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));

        // update conversation
        DateTime timeNow = now();
        DefaultMutableConversation conversation = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation.applyCommand(newAddMessageCommand(conversationId1, "msg456", timeNow));
        conversation.commit(conversationRepository, conversationEventListeners);

        conversation.applyCommand(new ConversationDeletedCommand(conversationId1, now()));
        conversation.commit(this.conversationRepository, conversationEventListeners);

        List<ConversationEventIdx> conversationEventIds = getConversationRepository().streamConversationEventIdxsByHour(timeNow)
                .collect(Collectors.toList());
        assertTrue(conversationEventIds.isEmpty());
    }

    private NewConversationCommand newConversationCommand(String convId) {
        return NewConversationCommandBuilder.aNewConversationCommand(convId).
                withAdId("m123456").
                withBuyer("buyer@hotmail.com", conversationId1BuyerSecret).
                withSeller("seller@gmail.com", conversationId1SellerSecret).
                withCreatedAt(new DateTime(2012, 2, 10, 9, 11, 43)).
                withState(ConversationState.ACTIVE).
                addCustomValue("L1-CATEGORY-ID", "41").
                build();
    }

    private AddMessageCommand newAddMessageCommand(String convId, String msgId, DateTime receivedAtDateTime) {
        return AddMessageCommandBuilder.anAddMessageCommand(convId, msgId).
                withMessageDirection(MessageDirection.SELLER_TO_BUYER).
                withReceivedAt(receivedAtDateTime).
                addHeader("From", "buyer@hotmail.com").
                addHeader("To", "9y3k9x6cvm8dp@platform.ebay.com").
                build();
    }
}