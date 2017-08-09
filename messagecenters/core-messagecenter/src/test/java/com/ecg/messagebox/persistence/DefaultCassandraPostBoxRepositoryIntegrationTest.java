package com.ecg.messagebox.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.jsonconverter.JsonConverter;
import com.ecg.messagebox.persistence.model.PaginatedConversationIds;
import com.ecg.messagebox.util.EmptyConversationFixture;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.MessageType.CHAT;
import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

public class DefaultCassandraPostBoxRepositoryIntegrationTest {

    private static final String UID1 = "u1";
    private static final String UID2 = "u2";
    private static final String UID5 = "u5";
    private static final String UID4 = "u4";
    private static final String CID = "c1";
    private static final String ADID = "a1";
    private static final Optional<String> NO_MSG_ID_CURSOR = empty();
    private static final int MSGS_LIMIT = 100;

    private static DefaultCassandraPostBoxRepository conversationsRepo;

    private static CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();
    private static String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
    private static Session session;

    @BeforeClass
    public static void init() {
        try {
            session = casdb.loadSchema(keyspace, "cassandra_messagebox_schema.cql", "cassandra_schema.cql");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        JsonConverter jsonConverter = new JsonConverter(new JacksonAwareObjectMapperConfigurer());
        conversationsRepo = new DefaultCassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE, jsonConverter);
    }

    @After
    public void cleanup() {
        casdb.cleanTables(session, keyspace);
    }

    @Test
    public void getConversationsAlwaysReturnsNotNull() {
        PostBox nonExistent = conversationsRepo.getPostBox("nonexistent", ACTIVE, 0, 10);
        assertNotNull(nonExistent);
        assertTrue("nonexistent".equals(nonExistent.getUserId()));
        assertNotNull(nonExistent.getConversations());
        assertTrue(nonExistent.getConversations().isEmpty());
    }

    @Test
    public void getConversations() throws Exception {
        ConversationThread c1 = insertConversationWithMessages(UID1, UID2, CID, ADID, 2);

        insertConversationWithMessages(UID1, "u3", "c2", "a2", 3);
        conversationsRepo.changeConversationVisibilities(UID1,
                ImmutableMap.<String, String>builder().put("c2", "a2").build(),
                Visibility.ARCHIVED);

        ConversationThread c3 = insertConversationWithMessages(UID1, "u4", "c3", "a3", 15);

        ConversationThread c4 = insertConversationWithMessages(UID1, "u5", "c4", "a4", 5);
        conversationsRepo.resetConversationUnreadCount(UID1, "u5","c4", "a4");

        insertConversationWithMessages(UID2, "u6", "c5", "a5", 2);

        PostBox actualPostBoxUser = conversationsRepo.getPostBox(UID1, Visibility.ACTIVE, 0, 50);

        List<ConversationThread> expectedConversationsUser = newArrayList(
                new ConversationThread(c4).addNumUnreadMessages(UID5, 0).addNumUnreadMessages(UID1, 0)
                        .addMessages(Collections.emptyList()),
                new ConversationThread(c3).addNumUnreadMessages(UID4, 0).addNumUnreadMessages(UID1, 15).addMessages(Collections.emptyList()),
                new ConversationThread(c1).addNumUnreadMessages(UID1, 2).addNumUnreadMessages(UID2, 0).addMessages(Collections.emptyList()));
        PostBox expectedPostBoxUser = new PostBox(UID1, expectedConversationsUser, new UserUnreadCounts(UID1, 2, 17), 3);

        assertEquals(expectedPostBoxUser, actualPostBoxUser);
    }

    @Test
    public void getConversationMessageNotification() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);

        assertEquals(MessageNotification.RECEIVE, conversationsRepo.getConversationMessageNotification(UID1, CID).get());
        assertEquals(MessageNotification.RECEIVE, conversationsRepo.getConversationMessageNotification(UID2, CID).get());
    }

    @Test
    public void getConversationWithMessages_allMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        Optional<ConversationThread> actual = conversationsRepo.getConversationWithMessages(UID1, CID, of(timeBased().toString()), MSGS_LIMIT);

        assertEquals(true, actual.isPresent());
        assertEquals(newConversation, actual.get());
    }

    @Test
    public void getConversationWithMessages_someMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        ConversationThread expected = new ConversationThread(newConversation)
                .addMessages(newConversation.getMessages().subList(30, 40));

        UUID uuid = newConversation.getMessages().get(40).getId();
        Optional<ConversationThread> actual = conversationsRepo.getConversationWithMessages(UID1, CID, of(uuid.toString()), 10);

        assertEquals(true, actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    public void getConversationWithMessagesAndSystemMessage_someMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        List<Message> messages = newConversation.getMessages().subList(41, 50);

        Message systemMessage = new Message(UUIDs.timeBased(), "text", "-1", MessageType.SYSTEM_MESSAGE, "custom data");

        conversationsRepo.addSystemMessage(UID1, CID, ADID, systemMessage);

        Optional<ConversationThread> actual = conversationsRepo.getConversationWithMessages(UID1, CID, NO_MSG_ID_CURSOR, 10);

        assertEquals(true, actual.isPresent());
        assertEquals(messages, actual.get().getMessages().subList(0, 9));
        assertEquals(systemMessage, actual.get().getMessages().get(9));
    }

    @Test
    public void getConversationWithMessages_noMessages() throws Exception {
        UUID oldUuid = UUIDs.startOf(DateTime.now().minusSeconds(1).getMillis());
        insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        assertEquals(true,
                conversationsRepo.getConversationWithMessages(UID1, CID, of(oldUuid.toString()), 10).get().getMessages().isEmpty());
        assertEquals(true,
                conversationsRepo.getConversationWithMessages(UID2, CID, of(timeBased().toString()), 10).isPresent());
    }

    @Test
    public void getConversationMessages_someMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        List<Message> expected = newConversation.getMessages().subList(30, 40);

        UUID uuid = newConversation.getMessages().get(40).getId();
        List<Message> actual = conversationsRepo.getConversationMessages(UID1, CID, of(uuid.toString()), 10);

        assertEquals(expected, actual);
    }

    @Test
    public void getConversationMessages_noMessages() throws Exception {
        UUID oldUuid = UUIDs.startOf(DateTime.now().minusSeconds(1).getMillis());
        insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        assertEquals(true,
                conversationsRepo.getConversationMessages(UID1, CID, of(oldUuid.toString()), 10).isEmpty());
    }

    @Test
    public void getPaginatedConversationIds() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 2);

        insertConversationWithMessages(UID1, "u3", "c2", "a2", 3);

        conversationsRepo.changeConversationVisibilities(UID1,
                ImmutableMap.<String, String>builder().put("c2", "a2").build(),
                Visibility.ARCHIVED);

        insertConversationWithMessages(UID1, "u4", "c3", "a3", 15);

        insertConversationWithMessages(UID1, "u5", "c4", "a4", 5);

        conversationsRepo.resetConversationUnreadCount(UID1, "u5", "c4", "a4");

        insertConversationWithMessages(UID2, "u6", "c5", "a5", 2);

        PaginatedConversationIds actualPaginatedConversationIds = conversationsRepo.getPaginatedConversationIds(UID1, Visibility.ACTIVE, 0, 50);

        PaginatedConversationIds expectedPaginatedConversationIds = new PaginatedConversationIds(Arrays.asList("c4", "c3", "c1"), 3);

        assertEquals(expectedPaginatedConversationIds, actualPaginatedConversationIds);
    }

    @Test
    public void getUserUnreadCounts() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);
        insertConversationWithMessages(UID1, "u3", "c2", "a2", 10);
        insertConversationWithMessages(UID1, "u4", "c3", "a3", 5);
        conversationsRepo.addMessage(UID1, CID, ADID, new Message(timeBased(), "message-5", UID2, CHAT), false);

        assertEquals(3, conversationsRepo.getUserUnreadCounts(UID1).getNumUnreadConversations());
        assertEquals(19, conversationsRepo.getUserUnreadCounts(UID1).getNumUnreadMessages());
    }

    @Test
    public void getConversationUnreadCount() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 1);
        assertEquals(1, conversationsRepo.getConversationUnreadCount(UID1, CID));

        conversationsRepo.addMessage(UID1, CID, ADID, new Message(timeBased(), "message-2", UID2, CHAT), true);
        conversationsRepo.addMessage(UID1, CID, ADID, new Message(timeBased(), "message-3", UID1, CHAT), true);
        assertEquals(3, conversationsRepo.getConversationUnreadCount(UID1, CID));

        conversationsRepo.addMessage(UID1, CID, ADID, new Message(timeBased(), "message-4", UID2, CHAT), false);
        assertEquals(3, conversationsRepo.getConversationUnreadCount(UID1, CID));
    }

    @Test
    public void resetConversationUnreadCount() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 20);
        assertEquals(20, conversationsRepo.getConversationUnreadCount(UID1, CID));
        assertEquals(20, conversationsRepo.getConversationOtherParticipantUnreadCount(UID2, CID));

        conversationsRepo.resetConversationUnreadCount(UID1, UID2, CID, ADID);

        assertEquals(0, conversationsRepo.getConversationUnreadCount(UID1, CID));
        assertEquals(0, conversationsRepo.getConversationOtherParticipantUnreadCount(UID2, CID));
    }

    @Test
    public void changeConversationVisibility() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 1);
        ConversationThread newConversation = conversationsRepo.getConversationWithMessages(UID1, CID, NO_MSG_ID_CURSOR, MSGS_LIMIT).get();
        assertEquals(Visibility.ACTIVE, newConversation.getVisibility());
        assertEquals(1, newConversation.getNumUnreadMessages(UID1));

        conversationsRepo.changeConversationVisibilities(UID1,
                ImmutableMap.<String, String>builder().put(CID, ADID).build(),
                Visibility.ARCHIVED);

        ConversationThread actual = conversationsRepo.getConversationWithMessages(UID1, CID, NO_MSG_ID_CURSOR, MSGS_LIMIT).get();
        assertEquals(Visibility.ARCHIVED, actual.getVisibility());
        assertEquals(0, actual.getNumUnreadMessages(UID1));
    }

    @Test
    public void deleteConversation() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);
        insertConversationWithMessages(UID2, "u3", "c2", "a2", 10);
        insertConversationWithMessages(UID1, "u3", "c4", "a4", 4);

        conversationsRepo.deleteConversation(UID1, CID, ADID);

        assertEquals(false, conversationsRepo.getConversationWithMessages(UID1, CID, NO_MSG_ID_CURSOR, MSGS_LIMIT).isPresent());
        assertEquals(true, conversationsRepo.getConversationWithMessages(UID2, "c2", NO_MSG_ID_CURSOR, MSGS_LIMIT).isPresent());
        assertEquals(1, conversationsRepo.getPaginatedConversationIds(UID1, Visibility.ACTIVE, 0, 100).getConversationsTotalCount());
        assertEquals(4, conversationsRepo.getUserUnreadCounts(UID1).getNumUnreadMessages());
        assertEquals(true, conversationsRepo.getConversationWithMessages(UID1, "c4", NO_MSG_ID_CURSOR, MSGS_LIMIT).isPresent());
        assertEquals(false, conversationsRepo.getConversationWithMessages(UID1, CID, NO_MSG_ID_CURSOR, MSGS_LIMIT).isPresent());
    }

    @Test
    public void getConversationAdIdsMap() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);
        insertConversationWithMessages(UID2, "u3", "c2", "a2", 10);
        insertConversationWithMessages(UID1, "u4", "c3", "a3", 5);
        insertConversationWithMessages(UID1, "u3", "c4", "a4", 4);

        Map<String, String> conversationAdIdsMap = conversationsRepo.getConversationAdIdsMap(UID1, Arrays.asList(CID, "c4"));

        assertEquals(2, conversationAdIdsMap.size());
        assertEquals(ADID, conversationAdIdsMap.get(CID));
        assertEquals("a4", conversationAdIdsMap.get("c4"));
    }

    @Test
    public void getLastConversationModification() throws Exception {
        ConversationThread c1 = insertConversationWithMessages(UID1, UID2, CID, ADID, 5);

        ConversationModification lastConversationModification = conversationsRepo.getLastConversationModification(UID1, CID);

        assertEquals(c1.getLatestMessage().getId(), lastConversationModification.getMessageId());
    }

    @Test
    public void createEmptyConversationAndThenInsertAMessage() throws Exception {

        /**
         * When an empty conversation is created
         * Then a conversation with no messages is returned
         */
        String newConversationId = guid();

        String resultConversationId = conversationsRepo.createEmptyConversationProjection(EmptyConversationFixture.validEmptyConversationRequest(ADID, UID1, UID2), newConversationId, UID1);

        assertEquals("creation should be successful", newConversationId, resultConversationId);

        List<String> conversationIds = conversationsRepo.resolveConversationIdsByUserIdAndAdId(UID1, ADID, 1);

        assertTrue("New empty conversation should exists in list", conversationIds.contains(resultConversationId));

        Optional<ConversationThread> conversationThread = conversationsRepo.getConversationWithMessages(UID1, resultConversationId, of(timeBased().toString()), MSGS_LIMIT);

        assertEquals("conversation thread should be present",true, conversationThread.isPresent());
        assertEquals("conversation thread messages should be empty", 0, conversationThread.get().getMessages().size());

        /**
         * Given an empty conversation
         * When a new message is added to an empty conversation
         * Then a message is added to conversation
         */
        insertConversationWithMessages(UID1, UID2, resultConversationId, ADID, 1);

        conversationThread = conversationsRepo.getConversationWithMessages(UID1, resultConversationId, of(timeBased().toString()), MSGS_LIMIT);

        assertEquals("conversation thread should be present",true, conversationThread.isPresent());
        assertEquals("conversation thread should have 1 message", 1, conversationThread.get().getMessages().size());

    }

    private ConversationThread insertConversationWithMessages(String userId1, String userId2,
                                                              String convId, String adId, int numMessages
    ) throws Exception {
        HashMap<DateTime, Integer> datesWithCounts = new HashMap<>();
        datesWithCounts.put(DateTime.now(), numMessages);
        return insertConversationWithMessagesByDate(userId1, userId2, convId, adId, datesWithCounts);
    }

    private static ConversationThread insertConversationWithMessagesByDate(String userId1, String userId2, String convId,
                                                                           String adId, Map<DateTime, Integer> datesWithCounts)
            throws Exception {
        List<Message> messages = new ArrayList<>();

        for (Entry<DateTime, Integer> dateWithCount : datesWithCounts.entrySet()) {
            List<Message> newMessages = IntStream
                    .range(0, dateWithCount.getValue())
                    .mapToObj(i -> {
                        return new Message(UUIDs.startOf(dateWithCount.getKey().getMillis() + i),
                                CHAT,
                                new MessageMetadata("message-" + (i + 1), userId2, "custom-" + (i + 1))
                        );
                    })
                    .collect(Collectors.toList());

            messages.addAll(newMessages);
        }

        ConversationThread conversation = new ConversationThread(
                convId, adId, UID1, ACTIVE, RECEIVE,
                newArrayList(
                        new Participant(userId1, "user name 1", "u1@test.nl", BUYER),
                        new Participant(userId2, "user name 2", "u2@test.nl", SELLER)
                ),
                messages.get(messages.size() - 1), new ConversationMetadata(now(), "email subject", "conversation title"));
        conversation.addNumUnreadMessages(userId1, messages.size());
        conversation.addNumUnreadMessages(userId2, 0);
        conversation.addMessages(messages);

        conversationsRepo.createConversation(userId1, conversation, messages.get(0), true);
        conversationsRepo.createConversation(userId2, conversation, messages.get(0), false);
        messages.subList(1, messages.size()).forEach(message -> conversationsRepo.addMessage(userId1, convId, adId, message, true));
        messages.subList(1, messages.size()).forEach(message -> conversationsRepo.addMessage(userId2, convId, adId, message, false));

        return conversation;
    }

    private String guid() {
        Guids guids = new Guids();
        return guids.nextGuid();
    }
}