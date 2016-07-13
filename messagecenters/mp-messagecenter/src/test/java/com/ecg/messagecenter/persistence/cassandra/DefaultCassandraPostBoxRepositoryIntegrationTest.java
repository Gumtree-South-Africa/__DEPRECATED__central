package com.ecg.messagecenter.persistence.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagecenter.persistence.*;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.jayway.awaitility.Duration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

public class DefaultCassandraPostBoxRepositoryIntegrationTest {

    private static final String FOO_BAR_POST_BOX_ID = "foo@bar.com";
    private static final String BAR_FOO_POST_BOX_ID = "bar@foo.com";

    private static CassandraPostBoxRepository postBoxRepository;

    private static CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();
    private static String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
    private static Session session;

    @BeforeClass
    public static void init() {
        try {
            session = casdb.loadSchema(keyspace, "cassandra_messagebox_schema.cql");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        DefaultCassandraPostBoxRepository repo = new DefaultCassandraPostBoxRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE, 30);

        repo.setObjectMapperConfigurer(new JacksonAwareObjectMapperConfigurer());
        postBoxRepository = repo;
    }

    @After
    public void cleanup() {
        casdb.cleanTables(session, keyspace);
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void getByIdAlwaysReturnsNotNull() {
        PostBox nonExistent = postBoxRepository.getPostBox("nonexistent");
        assertNotNull(nonExistent);
        assertNotNull(nonExistent.getNewRepliesCounter());
        assertEquals(0, nonExistent.getNewRepliesCounter());
        assertNotNull(nonExistent.getConversationThreads());
        assertTrue(nonExistent.getConversationThreads().isEmpty());
    }

    @Test
    public void conversationUnreadCount() {
        PostBox postBox = createPostBox(3, FOO_BAR_POST_BOX_ID);
        postBox.getConversationThreads().forEach(ct -> postBoxRepository.addReplaceConversationThread(FOO_BAR_POST_BOX_ID, ct));

        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId1");

        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2");
        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2");
        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2");

        assertEquals(0, postBoxRepository.getConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId0"));
        assertEquals(0, postBoxRepository.getConversationThread(FOO_BAR_POST_BOX_ID, "conversationId0").get().getNumUnreadMessages());
        assertEquals(1, postBoxRepository.getConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId1"));
        assertEquals(1, postBoxRepository.getConversationThread(FOO_BAR_POST_BOX_ID, "conversationId1").get().getNumUnreadMessages());
        assertEquals(3, postBoxRepository.getConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2"));
        assertEquals(3, postBoxRepository.getConversationThread(FOO_BAR_POST_BOX_ID, "conversationId2").get().getNumUnreadMessages());

        postBoxRepository.resetConversationUnreadMessagesCountAsync(FOO_BAR_POST_BOX_ID, "conversationId0");
        postBoxRepository.resetConversationUnreadMessagesCountAsync(FOO_BAR_POST_BOX_ID, "conversationId1");
        postBoxRepository.resetConversationUnreadMessagesCountAsync(FOO_BAR_POST_BOX_ID, "conversationId2");

        await()
                .pollInterval(fibonacci())
                .atMost(Duration.TWO_SECONDS)
                .until(() -> postBoxRepository.getUnreadCounts(FOO_BAR_POST_BOX_ID).getNumUnreadConversations() == 0);

        assertEquals(0, postBoxRepository.getConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId0"));
        assertEquals(0, postBoxRepository.getConversationThread(FOO_BAR_POST_BOX_ID, "conversationId0").get().getNumUnreadMessages());
        assertEquals(0, postBoxRepository.getConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId1"));
        assertEquals(0, postBoxRepository.getConversationThread(FOO_BAR_POST_BOX_ID, "conversationId1").get().getNumUnreadMessages());
        assertEquals(0, postBoxRepository.getConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2"));
        assertEquals(0, postBoxRepository.getConversationThread(FOO_BAR_POST_BOX_ID, "conversationId2").get().getNumUnreadMessages());
    }

    @Test
    public void postBoxUnreadCounts() {
        PostBox postBox = createPostBox(3, FOO_BAR_POST_BOX_ID);
        postBox.getConversationThreads().forEach(ct -> postBoxRepository.addReplaceConversationThread(FOO_BAR_POST_BOX_ID, ct));

        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId1");

        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2");
        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2");
        postBoxRepository.incrementConversationUnreadMessagesCount(FOO_BAR_POST_BOX_ID, "conversationId2");

        PostBoxUnreadCounts postBoxUnreadCounts = postBoxRepository.getUnreadCounts(FOO_BAR_POST_BOX_ID);
        assertEquals(2, postBoxUnreadCounts.getNumUnreadConversations());
        assertEquals(4, postBoxUnreadCounts.getNumUnreadMessages());

        PostBox reloadedPostBox = postBoxRepository.getPostBox(FOO_BAR_POST_BOX_ID);
        assertEquals(2, reloadedPostBox.getNumUnreadConversations());
        assertEquals(4, reloadedPostBox.getNewRepliesCounter());

        assertEquals(0, reloadedPostBox.getConversationThreads().get(0).getNumUnreadMessages());
        assertEquals(1, reloadedPostBox.getConversationThreads().get(1).getNumUnreadMessages());
        assertEquals(3, reloadedPostBox.getConversationThreads().get(2).getNumUnreadMessages());

        postBoxUnreadCounts = postBoxRepository.getUnreadCounts(BAR_FOO_POST_BOX_ID);

        postBoxRepository.getConversationThreadIds(BAR_FOO_POST_BOX_ID);

        assertEquals(0, postBoxUnreadCounts.getNumUnreadConversations());
        assertEquals(0, postBoxUnreadCounts.getNumUnreadMessages());

        postBoxRepository.resetConversationUnreadMessagesCountAsync(FOO_BAR_POST_BOX_ID, "conversationId1");
        postBoxRepository.resetConversationUnreadMessagesCountAsync(FOO_BAR_POST_BOX_ID, "conversationId2");

        await()
                .pollInterval(fibonacci())
                .atMost(Duration.TWO_SECONDS)
                .until(() -> postBoxRepository.getUnreadCounts(FOO_BAR_POST_BOX_ID).getNumUnreadConversations() == 0);

        postBoxUnreadCounts = postBoxRepository.getUnreadCounts(FOO_BAR_POST_BOX_ID);
        assertEquals(0, postBoxUnreadCounts.getNumUnreadConversations());
        assertEquals(0, postBoxUnreadCounts.getNumUnreadMessages());

        reloadedPostBox = postBoxRepository.getPostBox(FOO_BAR_POST_BOX_ID);
        assertEquals(0, reloadedPostBox.getNumUnreadConversations());
        assertEquals(0, reloadedPostBox.getNewRepliesCounter());

        assertEquals(0, reloadedPostBox.getConversationThreads().get(0).getNumUnreadMessages());
        assertEquals(0, reloadedPostBox.getConversationThreads().get(1).getNumUnreadMessages());
        assertEquals(0, reloadedPostBox.getConversationThreads().get(2).getNumUnreadMessages());
    }

    @Test
    public void shouldAddOrUpdateResponseData() {
        DateTime creationDate = DateTime.now();
        ResponseData expectedResponseData = new ResponseData("userId", "conversationId1", creationDate, MessageType.ASQ, 50);

        postBoxRepository.addOrUpdateResponseDataAsync(expectedResponseData);

        await()
                .pollInterval(fibonacci())
                .atMost(Duration.TWO_SECONDS)
                .until(() -> postBoxRepository.getResponseData("userId").size() == 1);

        List<ResponseData> responseDataList = postBoxRepository.getResponseData("userId");

        assertEquals(expectedResponseData, responseDataList.get(0));
    }

    private PostBox createPostBox(int numConversations, String postBoxId) {
        List<ConversationThread> conversations = new ArrayList<>();
        for (int i = 0; i < numConversations; i++) {
            DateTime receivedAt = now().minusMinutes(i + 1);

            ConversationThread ct = new ConversationThread(
                    "adId", "conversationId" + i,
                    receivedAt, receivedAt, receivedAt, i,
                    Optional.of("Message"), Optional.of("buyer name"), Optional.of("seller name"),
                    Optional.of("buyerId"), Optional.of("sellerId"), Optional.empty(),
                    Optional.empty(), Optional.of(receivedAt));
            conversations.add(ct);
        }
        return new PostBox(postBoxId, conversations);
    }

}