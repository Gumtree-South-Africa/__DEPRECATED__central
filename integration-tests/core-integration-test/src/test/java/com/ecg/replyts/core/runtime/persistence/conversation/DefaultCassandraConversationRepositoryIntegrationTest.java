package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationModificationDate;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventId;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
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
    public void shouldStoreConversationEventByDay() {
        DateTime timeNow = now();
        DateTime firstMsgReceivedAtDateTime = new DateTime(timeNow.getYear(), timeNow.getMonthOfYear(), timeNow.getDayOfMonth(), 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));
        List<ConversationEventId> conversationEvents = getConversationRepository().streamConversationEventIdsByHour(timeNow)
                .collect(Collectors.toList());
        assertEquals(1, conversationEvents.size());
    }

    @Test
    public void shouldGetLastModificationIdx() throws InterruptedException {
        // create conversation
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));
        assertEquals(firstMsgReceivedAtDateTime, getConversationRepository().getLastModifiedDate(conversationId1));

        // update conversation
        DateTime secondMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 12, 22, 5);
        DefaultMutableConversation conversation = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation.applyCommand(newAddMessageCommand(conversationId1, "msg456", secondMsgReceivedAtDateTime));
        conversation.commit(conversationRepository, conversationEventListeners);

        assertEquals(secondMsgReceivedAtDateTime, getConversationRepository().getLastModifiedDate(conversationId1));
        assertNull(getConversationRepository().getLastModifiedDate(conversationId2));
    }

    @Test
    public void shouldDeleteOldConversationModificationIdx() {
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        ConversationModificationDate firstConversationModificationDate = new ConversationModificationDate(conversationId1, firstMsgReceivedAtDateTime);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));

        getConversationRepository().deleteOldConversationModificationDate(firstConversationModificationDate);

        List<ConversationModificationDate> conversationModificationDates = getConversationRepository().streamConversationModificationsByDay(2012, 2, 10)
                .collect(Collectors.toList());
        assertTrue(conversationModificationDates.isEmpty());
        assertNull(getConversationRepository().getLastModifiedDate(conversationId1));
    }

    @Test
    public void shouldStreamConversationModificationsByDay() {
        // create conversation
        DateTime firstMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 9, 11, 43);
        ConversationModificationDate firstConversationModificationDate = new ConversationModificationDate(conversationId1, firstMsgReceivedAtDateTime);
        given(newConversationCommand(conversationId1), newAddMessageCommand(conversationId1, "msg123", firstMsgReceivedAtDateTime));

        // update conversation
        DateTime secondMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 12, 22, 5);
        ConversationModificationDate secondConversationModificationDate = new ConversationModificationDate(conversationId1, secondMsgReceivedAtDateTime);
        DefaultMutableConversation conversation = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation.applyCommand(newAddMessageCommand(conversationId1, "msg456", secondMsgReceivedAtDateTime));
        conversation.commit(conversationRepository, conversationEventListeners);

        List<ConversationModificationDate> conversationModificationDates = getConversationRepository().streamConversationModificationsByDay(2012, 2, 10)
                .collect(Collectors.toList());
        assertEquals(2, conversationModificationDates.size());
        assertTrue(conversationModificationDates.contains(firstConversationModificationDate));
        assertTrue(conversationModificationDates.contains(secondConversationModificationDate));
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
        DateTime secondMsgReceivedAtDateTime = new DateTime(2012, 2, 10, 12, 22, 5);
        DefaultMutableConversation conversation = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation.applyCommand(newAddMessageCommand(conversationId1, "msg456", secondMsgReceivedAtDateTime));
        conversation.commit(conversationRepository, conversationEventListeners);

        conversation.applyCommand(new ConversationDeletedCommand(conversationId1, now()));
        conversation.commit(this.conversationRepository, conversationEventListeners);

        List<ConversationModificationDate> conversationModificationDates = getConversationRepository().streamConversationModificationsByDay(2012, 2, 10)
                .collect(Collectors.toList());
        assertTrue(conversationModificationDates.isEmpty());
        assertNull(getConversationRepository().getLastModifiedDate(conversationId1));
    }

    @Test
    public void deleteConversationCommandDeletesConversationEventsByDay() {
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

        List<ConversationEventId> conversationEventIds = getConversationRepository().streamConversationEventIdsByHour(timeNow)
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