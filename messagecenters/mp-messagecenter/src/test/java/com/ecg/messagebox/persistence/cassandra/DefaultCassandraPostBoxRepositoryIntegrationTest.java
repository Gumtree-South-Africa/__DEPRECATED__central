package com.ecg.messagebox.persistence.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagebox.json.JsonConverter;
import com.ecg.messagebox.model.*;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.google.common.collect.ImmutableMap;
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
            session = casdb.loadSchema(keyspace, "cassandra_new_messagebox_schema.cql");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        JsonConverter jsonConverter = new JsonConverter(new JacksonAwareObjectMapperConfigurer());
        conversationsRepo = new DefaultCassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE, 100, jsonConverter);
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
                new ConversationThread(c1).addMessages(Collections.emptyList()),
                new ConversationThread(c3).addMessages(Collections.emptyList()),
                new ConversationThread(c4).addNumUnreadMessages(0).addMessages(Collections.emptyList())
        );
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
                .addMessages(newConversation.getMessages().subList(2, 12));

        UUID uuid = newConversation.getMessages().get(1).getId();
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

    private ConversationThread insertConversationWithMessages(String userId1, String userId2,
                                                              String convId, String adId, int numMessages)
            throws Exception {

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

        List<Message> conversationMessages = new ArrayList<>(messages);
        Collections.reverse(conversationMessages);
        conversation.addMessages(conversationMessages);

        conversationsRepo.createConversation(userId1, conversation, messages.get(0), true);
        messages.subList(1, numMessages).forEach(message -> conversationsRepo.addMessage(userId1, convId, adId, message, true));

        return conversation;
    }
}