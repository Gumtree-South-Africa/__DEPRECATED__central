package com.ecg.messagebox.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.jsonconverter.JsonConverter;
import com.ecg.messagebox.persistence.model.PaginatedConversationIds;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.Map.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.MessageType.CHAT;
import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static com.google.common.collect.Lists.newArrayList;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

public class DefaultCassandraPostBoxRepositoryIntegrationTest {

    private static final String UID1 = "u1";
    private static final String UID2 = "u2";
    private static final String CID = "c1";
    private static final String ADID = "a1";

    private static DefaultCassandraPostBoxRepository conversationsRepo;

    private static CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();
    private static String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
    private static Session session;

    @BeforeClass
    public static void init() {
        try {
            session = casdb.loadSchema(keyspace, "cassandra_new_messagebox_schema.cql", "cassandra_schema.cql");
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
        conversationsRepo.resetConversationUnreadCount(UID1, "c4", "a4");

        insertConversationWithMessages(UID2, "u6", "c5", "a5", 2);

        PostBox actualPostBox = conversationsRepo.getPostBox(UID1, Visibility.ACTIVE, 0, 50);

        List<ConversationThread> expectedConversations = newArrayList(
                new ConversationThread(c4).addNumUnreadMessages(0).addMessages(Collections.emptyList()),
                new ConversationThread(c3).addMessages(Collections.emptyList()),
                new ConversationThread(c1).addMessages(Collections.emptyList()));
        PostBox expectedPostBox = new PostBox(UID1, expectedConversations, new UserUnreadCounts(UID1, 3, 20), 3);

        assertEquals(expectedPostBox, actualPostBox);
    }

    @Test
    public void getConversation() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 4);

        ConversationThread expected = new ConversationThread(newConversation)
                .addMessages(Collections.emptyList())
                .addNumUnreadMessages(0);

        Optional<ConversationThread> actual = conversationsRepo.getConversation(UID1, CID);

        assertEquals(true, actual.isPresent());
        assertEquals(expected, actual.get());

        assertEquals(false, conversationsRepo.getConversation(UID2, CID).isPresent());
    }

    @Test
    public void getConversationWithMessages_allMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        Optional<ConversationThread> actual = conversationsRepo.getConversationWithMessages(UID1, CID, Optional.of(timeBased().toString()), 100);

        assertEquals(true, actual.isPresent());
        assertEquals(newConversation, actual.get());
    }

    @Test
    public void getConversationWithMessages_someMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        ConversationThread expected = new ConversationThread(newConversation)
                .addMessages(newConversation.getMessages().subList(30, 40));

        UUID uuid = newConversation.getMessages().get(40).getId();
        Optional<ConversationThread> actual = conversationsRepo.getConversationWithMessages(UID1, CID, Optional.of(uuid.toString()), 10);

        assertEquals(true, actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    public void getConversationWithMessages_noMessages() throws Exception {
        UUID oldUuid = UUIDs.startOf(DateTime.now().minusSeconds(1).getMillis());
        insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        assertEquals(true,
                conversationsRepo.getConversationWithMessages(UID1, CID, Optional.of(oldUuid.toString()), 10).get().getMessages().isEmpty());
        assertEquals(false,
                conversationsRepo.getConversationWithMessages(UID2, CID, Optional.of(timeBased().toString()), 10).isPresent());
    }

    @Test
    public void getConversationMessages_someMessages() throws Exception {
        ConversationThread newConversation = insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        List<Message> expected = newConversation.getMessages().subList(30, 40);

        UUID uuid = newConversation.getMessages().get(40).getId();
        List<Message> actual = conversationsRepo.getConversationMessages(UID1, CID, Optional.of(uuid.toString()), 10);

        assertEquals(expected, actual);
    }

    @Test
    public void getConversationMessages_noMessages() throws Exception {
        UUID oldUuid = UUIDs.startOf(DateTime.now().minusSeconds(1).getMillis());
        insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        assertEquals(true,
                conversationsRepo.getConversationMessages(UID1, CID, Optional.of(oldUuid.toString()), 10).isEmpty());
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

        conversationsRepo.resetConversationUnreadCount(UID1, "c4", "a4");

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

        conversationsRepo.resetConversationUnreadCount(UID1, CID, ADID);

        assertEquals(0, conversationsRepo.getConversationUnreadCount(UID1, CID));
    }

    @Test
    public void changeConversationVisibility() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 1);
        assertEquals(Visibility.ACTIVE, conversationsRepo.getConversation(UID1, CID).get().getVisibility());

        conversationsRepo.changeConversationVisibilities(UID1,
                ImmutableMap.<String, String>builder().put(CID, ADID).build(),
                Visibility.ARCHIVED);

        assertEquals(Visibility.ARCHIVED, conversationsRepo.getConversation(UID1, CID).get().getVisibility());
    }

    @Test
    public void deleteConversations() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);
        insertConversationWithMessages(UID2, "u3", "c2", "a2", 10);
        insertConversationWithMessages(UID1, "u4", "c3", "a3", 5);
        insertConversationWithMessages(UID1, "u3", "c4", "a4", 4);

        Map<String, String> adConversationIdsMap = new HashMap<>();
        adConversationIdsMap.put(ADID, CID);
        adConversationIdsMap.put("a3", "c3");

        conversationsRepo.deleteConversations(UID1, adConversationIdsMap);

        assertEquals(false, conversationsRepo.getConversation(UID1, CID).isPresent());
        assertEquals(false, conversationsRepo.getConversation(UID1, "c3").isPresent());
        assertEquals(true, conversationsRepo.getConversation(UID1, "c4").isPresent());
        assertEquals(true, conversationsRepo.getConversation(UID2, "c2").isPresent());
        assertEquals(1, conversationsRepo.getPaginatedConversationIds(UID1, Visibility.ACTIVE, 0, 100).getConversationsTotalCount());
        assertEquals(4, conversationsRepo.getUserUnreadCounts(UID1).getNumUnreadMessages());
        assertEquals(true, conversationsRepo.getConversationWithMessages(UID1, "c4", Optional.empty(), 10).isPresent());
        assertEquals(false, conversationsRepo.getConversationWithMessages(UID1, CID, Optional.empty(), 10).isPresent());
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
    public void getConversationModificationsByDate() throws Exception {
        Map<DateTime, Integer> datesWithCounts = new LinkedHashMap<>();
        datesWithCounts.put(DateTime.now().minusMonths(8), 7);
        datesWithCounts.put(DateTime.now(), 5);

        ConversationThread c1 = insertConversationWithMessagesByDate(UID1, UID2, CID, ADID, datesWithCounts);

        List<ConversationModification> newModList = conversationsRepo
                .getConversationModificationsByHour(c1.getLatestMessage().getReceivedDate().hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        List<ConversationModification> oldModList = conversationsRepo
                .getConversationModificationsByHour(c1.getMessages().get(0).getReceivedDate().hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        List<ConversationModification> noModList = conversationsRepo
                .getConversationModificationsByHour(c1.getMessages().get(0).getReceivedDate().plusHours(10).hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        assertEquals(5, newModList.size());
        assertEquals(7, oldModList.size());
        assertEquals(0, noModList.size());
    }

    @Test
    public void deleteModificationIndexByDate() throws Exception {
        ConversationThread c1 = insertConversationWithMessages(UID1, UID2, CID, ADID, 5);

        DateTime date = c1.getLatestMessage().getReceivedDate().hourOfDay().roundFloorCopy();
        UUID messageId = c1.getLatestMessage().getId();
        List<Message> messages = c1.getMessages();

        conversationsRepo.deleteModificationIndexByDate(date, messageId, UID1, CID);

        List<ConversationModification> modList = conversationsRepo
                .getConversationModificationsByHour(c1.getLatestMessage().getReceivedDate().hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        assertEquals(4, modList.size());
        assertEquals(true, modList.contains(new ConversationModification(UID1, CID, null, messages.get(3).getId(), date)));
        assertEquals(false, modList.contains(new ConversationModification(UID1, CID, null, messages.get(4).getId(), date)));
    }

    @Test
    public void getLastConversationModification() throws Exception {
        ConversationThread c1 = insertConversationWithMessages(UID1, UID2, CID, ADID, 5);

        ConversationModification lastConversationModification = conversationsRepo.getLastConversationModification(UID1, CID);

        assertEquals(c1.getLatestMessage().getId(), lastConversationModification.getMessageId());
    }

    private ConversationThread insertConversationWithMessages(String userId1, String userId2,
                                                              String convId, String adId, int numMessages
    ) throws Exception {
        HashMap<DateTime, Integer> datesWithCounts = new HashMap<>();
        datesWithCounts.put(DateTime.now(), numMessages);
        return insertConversationWithMessagesByDate(userId1, userId2, convId, adId, datesWithCounts);
    }

    private static ConversationThread insertConversationWithMessagesByDate(String userId1, String userId2,
                                                                           String convId, String adId, Map <DateTime, Integer> datesWithCounts)
            throws Exception {

        List<Message> messages = new ArrayList<>();

        for(Entry<DateTime, Integer> dateWithCount : datesWithCounts.entrySet()) {
            List<Message> newMessages = IntStream
                    .range(0, dateWithCount.getValue())
                    .mapToObj(i -> {
                        String senderUserId = i % 2 == 0 ? userId1 : userId2;
                        return new Message(UUIDs.startOf(dateWithCount.getKey().getMillis() + i),
                                CHAT,
                                new MessageMetadata("message-" + (i + 1), senderUserId, "custom-" + (i + 1))
                        );
                    })
                    .collect(Collectors.toList());

            messages.addAll(newMessages);
        }

        ConversationThread conversation = new ConversationThread(
                convId, adId, ACTIVE, RECEIVE,
                newArrayList(
                        new Participant(userId1, "user name 1", "u1@test.nl", BUYER),
                        new Participant(userId2, "user name 2", "u2@test.nl", SELLER)
                ),
                messages.get(messages.size() - 1), new ConversationMetadata(now(), "email subject"));
        conversation.addNumUnreadMessages(messages.size());
        conversation.addMessages(messages);

        conversationsRepo.createConversation(userId1, conversation, messages.get(0), true);
        messages.subList(1, messages.size()).forEach(message -> conversationsRepo.addMessage(userId1, convId, adId, message, true));

        return conversation;
    }
}