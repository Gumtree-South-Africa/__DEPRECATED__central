package com.ecg.replyts.core.runtime.persistence.conversation;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.app.preprocessorchain.preprocessors.IdBasedConversationResumer;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIndex;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierConfiguration;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

        ConversationResumer resumer = new IdBasedConversationResumer();

        ReflectionTestUtils.setField(resumer, "userIdentifierService", new UserIdentifierConfiguration().createUserIdentifierService());

        return new DefaultCassandraConversationRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE, resumer, 100, 5000, false);
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
        List<ConversationEventIndex> conversationEvents = getConversationRepository().streamConversationEventIndexesByHour(timeNow)
                .collect(Collectors.toList());
        assertEquals(1, conversationEvents.size());
        ConversationEventIndex conversationEventByHour = conversationEvents.get(0);
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

        List<ConversationEventIndex> conversationEventIdxs = getConversationRepository().streamConversationEventIndexesByHour(firstMsgReceivedAtDateTime.hourOfDay().roundFloorCopy())
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

        List<ConversationEventIndex> conversationEventIds = getConversationRepository().streamConversationEventIndexesByHour(timeNow)
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
