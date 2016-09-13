package com.ecg.messagebox.persistence.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.json.JsonConverter;
import com.ecg.messagebox.model.*;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.model.MessageNotification.RECEIVE;
import static com.ecg.messagebox.model.MessageType.CHAT;
import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.messagebox.model.Visibility.ACTIVE;
import static com.google.common.collect.Lists.newArrayList;
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
        conversationsRepo = new DefaultCassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE, jsonConverter, 100, 0, 1);
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
        PostBox expectedPostBox = new PostBox(UID1, expectedConversations, new PostBoxUnreadCounts(UID1, 3, 20));

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
        UUID oldUuid = timeBased();
        insertConversationWithMessages(UID1, UID2, CID, ADID, 50);

        assertEquals(true,
                conversationsRepo.getConversationWithMessages(UID1, CID, Optional.of(oldUuid.toString()), 10).get().getMessages().isEmpty());
        assertEquals(false,
                conversationsRepo.getConversationWithMessages(UID2, CID, Optional.of(timeBased().toString()), 10).isPresent());
    }

    @Test
    public void getUserUnreadCounts() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);
        insertConversationWithMessages(UID1, "u3", "c2", "a2", 10);
        insertConversationWithMessages(UID1, "u4", "c3", "a3", 5);
        conversationsRepo.addMessage(UID1, CID, ADID, new Message(timeBased(), "message-5", UID2, CHAT), false);

        assertEquals(3, conversationsRepo.getPostBoxUnreadCounts(UID1).getNumUnreadConversations());
        assertEquals(19, conversationsRepo.getPostBoxUnreadCounts(UID1).getNumUnreadMessages());
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

        Map<String,String> adConversationIdsMap = new HashMap<String,String>();
        adConversationIdsMap.put(ADID, CID);
        adConversationIdsMap.put("a3", "c3");

        conversationsRepo.deleteConversations(UID1, adConversationIdsMap);

        assertEquals(false, conversationsRepo.getConversation(UID1, CID).isPresent());
        assertEquals(false, conversationsRepo.getConversation(UID1, "c3").isPresent());
        assertEquals(true, conversationsRepo.getConversation(UID1, "c4").isPresent());
        assertEquals(true, conversationsRepo.getConversation(UID2, "c2").isPresent());
    }

    @Test
    public void deleteConversation() throws Exception {
        insertConversationWithMessages(UID1, UID2, CID, ADID, 4);
        insertConversationWithMessages(UID2, "u3", "c2", "a2", 10);

        conversationsRepo.deleteConversations(UID2, Collections.singletonMap("a2", "c2"));

        assertEquals(false, conversationsRepo.getConversation(UID2, "c3").isPresent());
        assertEquals(true, conversationsRepo.getConversation(UID1, CID).isPresent());
    }

    @Test
    public void getConversationAdIdsMap() throws Exception{
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
        ConversationThread c1 = insertConversationWithNewAndOldMessages(UID1, UID2, CID, ADID, 5, 7);

        List <ConversationModification> newModList = conversationsRepo
                .getConversationModificationsByHour(c1.getLatestMessage().getReceivedDate().hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        List <ConversationModification> oldModList = conversationsRepo
                .getConversationModificationsByHour(c1.getMessages().get(0).getReceivedDate().hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        List <ConversationModification> noModList = conversationsRepo
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

        List <ConversationModification> modList = conversationsRepo
                .getConversationModificationsByHour(c1.getLatestMessage().getReceivedDate().hourOfDay().roundFloorCopy())
                .collect(Collectors.toList());

        assertEquals(4, modList.size());
        assertEquals(true, modList.contains(new ConversationModification(UID1, CID, null, messages.get(3).getId(), date)));
        assertEquals(false, modList.contains(new ConversationModification(UID1, CID, null, messages.get(4).getId(), date)));
    }

    @Test
    public void blockUser() {
        conversationsRepo.blockUser("reporterId", "blockedId");

        Optional<BlockedUserInfo> blockedUserInfo = conversationsRepo.getBlockedUserInfo("reporterId", "blockedId");

        assertEquals(true, blockedUserInfo.isPresent());
        assertEquals("reporterId", blockedUserInfo.get().getReporterUserId());
        assertEquals("blockedId", blockedUserInfo.get().getBlockedUserId());
    }

    @Test
    public void unblockUser() {
        conversationsRepo.blockUser("reporterId", "blockedId");

        conversationsRepo.unblockUser("reporterId", "blockedId");

        Optional<BlockedUserInfo> blockedUserInfo = conversationsRepo.getBlockedUserInfo("reporterId", "blockedId");

        assertEquals(false, blockedUserInfo.isPresent());
    }

    @Test
    public void getLastConversationModification() throws Exception{
        ConversationThread c1 = insertConversationWithMessages(UID1, UID2, CID, ADID, 5);

        ConversationModification lastConversationModification = conversationsRepo.getLastConversationModification(UID1, CID);

        assertEquals(c1.getLatestMessage().getId(), lastConversationModification.getMessageId());
    }

    private ConversationThread insertConversationWithMessages(String userId1, String userId2,
                                                              String convId, String adId, int numMessages
    ) throws Exception {
        List<Message> messages = IntStream
                .range(0, numMessages)
                .mapToObj(i -> {
                    String senderUserId = i % 2 == 0 ? userId1 : userId2;
                    return new Message(timeBased(),
                            CHAT,
                            new MessageMetadata("message-" + (i + 1), senderUserId, "custom-" + (i + 1))
                    );
                })
                .collect(Collectors.toList());

        ConversationThread conversation = new ConversationThread(
                convId, adId, ACTIVE, RECEIVE,
                newArrayList(
                        new Participant(userId1, "user name 1", "u1@test.nl", BUYER),
                        new Participant(userId2, "user name 2", "u2@test.nl", SELLER)
                ),
                messages.get(numMessages - 1), new ConversationMetadata("email subject"));
        conversation.addNumUnreadMessages(numMessages);
        conversation.addMessages(messages);

        conversationsRepo.createConversation(userId1, conversation, messages.get(0), true);
        messages.subList(1, numMessages).forEach(message -> conversationsRepo.addMessage(userId1, convId, adId, message, true));

        return conversation;
    }


    private static ConversationThread insertConversationWithNewAndOldMessages(String userId1, String userId2,
                                                                              String convId, String adId, int numNewMessages, int numOldMessages)
        throws Exception {

        List<Message> messages = IntStream
                .range(0, numOldMessages)
                .mapToObj(i -> {
                    String senderUserId = i % 2 == 0 ? userId1 : userId2;
                    return new Message(UUIDs.startOf(DateTime.now().minusMonths(8).minusDays(i).getMillis() / 1000),
                            CHAT,
                            new MessageMetadata("message-" + (i + 1), senderUserId, "custom-" + (i + 1))
                    );
                })
                .collect(Collectors.toList());

        List<Message> newMessages = IntStream
                .range(0, numNewMessages)
                .mapToObj(i -> {
                    String senderUserId = i % 2 == 0 ? userId1 : userId2;
                    return new Message(timeBased(),
                            CHAT,
                            new MessageMetadata("message-" + (i + 1), senderUserId, "custom-" + (i + 1))
                    );
                })
                .collect(Collectors.toList());

        messages.addAll(newMessages);

        ConversationThread conversation = new ConversationThread(
                convId, adId, ACTIVE, RECEIVE,
                newArrayList(
                        new Participant(userId1, "user name 1", "u1@test.nl", BUYER),
                        new Participant(userId2, "user name 2", "u2@test.nl", SELLER)
                ),
                messages.get(messages.size() - 1), new ConversationMetadata("email subject"));
        conversation.addNumUnreadMessages(messages.size());
        conversation.addMessages(messages);

        conversationsRepo.createConversation(userId1, conversation, messages.get(0), true);
        messages.subList(1, messages.size()).forEach(message -> conversationsRepo.addMessage(userId1, convId, adId, message, true));

        return conversation;
    }
}