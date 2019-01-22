package com.ecg.messagecenter.core.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.Counter;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { CassandraSimplePostBoxRepositoryIntegrationTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.cassandra.conversation.class = com.ecg.messagecenter.core.persistence.simple.ConversationThread"
})
public class CassandraSimplePostBoxRepositoryIntegrationTest {

    @Autowired
    private SimpleMessageCenterRepository postBoxRepository;

    @Test
    public void persistsPostbox() {
        PostBox box = new PostBox<>("foo@bar.com", Optional.empty(), Collections.emptyList());

        postBoxRepository.write(box);

        assertNotNull(postBoxRepository.byId(PostBoxId.fromEmail("foo@bar.com")));
    }

    @Test
    public void persistsPostboxWithUnreadMessages() {
        String email = "unread-single-thread@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);
        PostBox box = new PostBox<>(email, Optional.empty(), Collections.emptyList());

        AbstractConversationThread convThread = createConversationThread(now(), "unread-single");

        postBoxRepository.write(box);

        // Add two unread messages to the conversation thread.
        postBoxRepository.upsertThread(postBoxId, convThread, true);
        postBoxRepository.upsertThread(postBoxId, convThread, true);

        // Retrieve the PostBox and the write it again - let see whether the value of unread messages has been written.
        PostBox retrievedPostBox = postBoxRepository.byId(postBoxId);
        postBoxRepository.write(retrievedPostBox);

        PostBox checkedPostBox = postBoxRepository.byId(postBoxId);
        assertEquals("PostBox should contain only one thread", 1, checkedPostBox.getConversationThreads().size());

        AbstractConversationThread retrievedThread = (AbstractConversationThread) checkedPostBox.getConversationThreads().get(0);
        assertTrue("Thread contains unread message", retrievedThread.isContainsUnreadMessages());
        assertEquals("PostBox conversation thread should have 2 unread messages", 2, postBoxRepository.unreadCountInConversation(postBoxId, retrievedThread.getConversationId()));
    }

    @Test
    public void persistsPostboxWithUnreadMessagesInMultipleConversation() {
        String email = "unread-multi-thread@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);
        createMultiThreadPostBox(email, "unread");

        // Retrieve the PostBox and the write it again - let see whether the value of unread messages has been written.
        PostBox retrievedPostBox = postBoxRepository.byId(postBoxId);
        postBoxRepository.write(retrievedPostBox);

        PostBox checkedPostBox = postBoxRepository.byId(postBoxId);
        assertEquals("PostBox should contain only three threads", 3, checkedPostBox.getConversationThreads().size());
        assertEquals("PostBox conversation thread should have 1 unread messages", 1, postBoxRepository.unreadCountInConversation(postBoxId, "unread-1"));
        assertEquals("PostBox conversation thread should have 2 unread messages", 2, postBoxRepository.unreadCountInConversation(postBoxId, "unread-2"));
        assertEquals("PostBox conversation thread should have 3 unread messages", 3, postBoxRepository.unreadCountInConversation(postBoxId, "unread-3"));
    }

    @Test
    public void deleteConversation() {
        String email = "delete-multi-thread@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);
        createMultiThreadPostBox(email, "delete");

        postBoxRepository.deleteConversations(new PostBox<>(email, Optional.empty(), Collections.emptyList()), Collections.singletonList("delete-2"));
        PostBox postBox = postBoxRepository.byId(postBoxId);

        assertEquals("PostBox should contain only two threads", 2, postBox.getConversationThreads().size());
        assertEquals("PostBox conversation thread should have 1 unread messages", 1, postBoxRepository.unreadCountInConversation(postBoxId, "delete-1"));
        assertEquals("PostBox conversation thread should have 3 unread messages", 3, postBoxRepository.unreadCountInConversation(postBoxId, "delete-3"));
        assertEquals("PostBox conversation thread should have 0 unread messages - does not exist", 0, postBoxRepository.unreadCountInConversation(postBoxId, "delete-2"));
    }

    @Test
    public void readSingleConversation() {
        String email = "read-single-thread@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);
        createMultiThreadPostBox(email, "read-single");

        AbstractConversationThread readConversation = postBoxRepository.threadById(postBoxId, "read-single-2").get();
        postBoxRepository.markConversationAsRead(new PostBox<>(email, Optional.empty(), Collections.singletonList(readConversation)), readConversation);
        PostBox postBox = postBoxRepository.byId(postBoxId);

        assertEquals("PostBox should contain three threads", 3, postBox.getConversationThreads().size());
        assertEquals("PostBox conversation thread should have 1 unread messages", 1, postBoxRepository.unreadCountInConversation(postBoxId, "read-single-1"));
        assertEquals("PostBox conversation thread should have 0 unread messages", 0, postBoxRepository.unreadCountInConversation(postBoxId, "read-single-2"));
        assertEquals("PostBox conversation thread should have 3 unread messages", 3, postBoxRepository.unreadCountInConversation(postBoxId, "read-single-3"));

        assertTrue("PostBox conversation thread should contain unread messages", postBoxRepository.threadById(postBoxId, "read-single-1").get().isContainsUnreadMessages());
        assertFalse("PostBox conversation thread should not contain unread messages", postBoxRepository.threadById(postBoxId, "read-single-2").get().isContainsUnreadMessages());
        assertTrue("PostBox conversation thread should contain unread messages", postBoxRepository.threadById(postBoxId, "read-single-3").get().isContainsUnreadMessages());
    }

    @Test
    public void readMultipleConversations() {
        String email = "read-multi-thread@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);
        createMultiThreadPostBox(email, "read-multi");

        AbstractConversationThread readConversation2 = postBoxRepository.threadById(postBoxId, "read-multi-2").get();
        AbstractConversationThread readConversation3 = postBoxRepository.threadById(postBoxId, "read-multi-3").get();
        postBoxRepository.markConversationsAsRead(new PostBox<>(email, Optional.empty(), Arrays.asList(readConversation2, readConversation3)), Arrays.asList(readConversation2, readConversation3));
        PostBox postBox = postBoxRepository.byId(postBoxId);

        assertEquals("PostBox should contain three threads", 3, postBox.getConversationThreads().size());
        assertEquals("PostBox conversation thread should have 1 unread messages", 1, postBoxRepository.unreadCountInConversation(postBoxId, "read-multi-1"));
        assertEquals("PostBox conversation thread should have 0 unread messages", 0, postBoxRepository.unreadCountInConversation(postBoxId, "read-multi-2"));
        assertEquals("PostBox conversation thread should have 0 unread messages", 0, postBoxRepository.unreadCountInConversation(postBoxId, "read-multi-3"));

        assertTrue("PostBox conversation thread should contain unread messages", postBoxRepository.threadById(postBoxId, "read-multi-1").get().isContainsUnreadMessages());
        assertFalse("PostBox conversation thread should not contain unread messages", postBoxRepository.threadById(postBoxId, "read-multi-2").get().isContainsUnreadMessages());
        assertFalse("PostBox conversation thread should not contain unread messages", postBoxRepository.threadById(postBoxId, "read-multi-3").get().isContainsUnreadMessages());
    }

    private void createMultiThreadPostBox(String email, String threadName) {
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread convThread1 = createConversationThread(now(), threadName + "-1");
        AbstractConversationThread convThread2 = createConversationThread(now(), threadName + "-2");
        AbstractConversationThread convThread3 = createConversationThread(now(), threadName + "-3");

        PostBox box = new PostBox<>(email, Optional.empty(), Arrays.asList(convThread1, convThread2, convThread3));
        postBoxRepository.write(box);

        postBoxRepository.upsertThread(postBoxId, convThread1, true);
        postBoxRepository.upsertThread(postBoxId, convThread2, true);
        postBoxRepository.upsertThread(postBoxId, convThread3, true);
        postBoxRepository.upsertThread(postBoxId, convThread3, true);
        postBoxRepository.upsertThread(postBoxId, convThread2, true);
        postBoxRepository.upsertThread(postBoxId, convThread3, true);
    }

    @Test
    public void upsertsThreadWithUnreadMessages() throws Exception {
        String email = "foo@bar.com";
        DateTime now = now();
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread convThread = createConversationThread(now.minusMinutes(1), "123");
        AbstractConversationThread updatedConvThread = createConversationThread(now, "123");

        PostBox box = new PostBox<>(email, Optional.empty(), Collections.singletonList(convThread));
        postBoxRepository.write(box);

        postBoxRepository.upsertThread(postBoxId, updatedConvThread, true);
        assertEquals("Conversation should contain 1 unread message", 1, postBoxRepository.unreadCountInConversation(postBoxId, updatedConvThread.getConversationId()));

        postBoxRepository.upsertThread(postBoxId, updatedConvThread, true);
        assertEquals("Conversation should contain 2 unread messages", 2, postBoxRepository.unreadCountInConversation(postBoxId, updatedConvThread.getConversationId()));

        postBoxRepository.upsertThread(postBoxId, updatedConvThread, true);
        assertEquals("Conversation should contain 3 unread messages", 3, postBoxRepository.unreadCountInConversation(postBoxId, updatedConvThread.getConversationId()));
    }

    @Test
    public void unreadCountInConversations() throws Exception {
        String email = "unreadCountInConversations@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread thread1 = createConversationThread(now(), "mark-conversation-1");
        postBoxRepository.upsertThread(postBoxId, thread1, true);
        postBoxRepository.upsertThread(postBoxId, thread1, true);
        AbstractConversationThread thread2 = createConversationThread(now(), "mark-conversation-2");
        postBoxRepository.upsertThread(postBoxId, thread2, true);
        AbstractConversationThread thread3 = createConversationThread(now(), "mark-conversation-3");

        List<AbstractConversationThread> conversations = Arrays.asList(thread1, thread2, thread3);
        PostBox box = new PostBox<>(email, Optional.empty(), conversations);
        postBoxRepository.write(box);

        int unreadCount = postBoxRepository.unreadCountInConversations(postBoxId, conversations);
        assertEquals(3, unreadCount);
    }


    @Test
    public void unreadCountInConversationsNewConversation() throws Exception {
        String email = "unreadCountInConversations@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread thread1 = createConversationThread(now(), "mark-conversation-1");
        postBoxRepository.upsertThread(postBoxId, thread1, true);
        postBoxRepository.upsertThread(postBoxId, thread1, true);
        AbstractConversationThread thread2 = createConversationThread(now(), "mark-conversation-2");
        postBoxRepository.upsertThread(postBoxId, thread2, true);
        AbstractConversationThread thread3 = createConversationThread(now(), "mark-conversation-3");

        List<AbstractConversationThread> conversations = Arrays.asList(thread1, thread2, thread3);
        PostBox box = new PostBox<>(email, Optional.empty(), Arrays.asList(thread1, thread2));
        postBoxRepository.write(box);

        int unreadCount = postBoxRepository.unreadCountInConversations(postBoxId, conversations);
        assertEquals(3, unreadCount);
    }


    @Test
    public void markConversationAsRead() throws Exception {
        String email = "markconversation@bar.com";
        DateTime now = now();
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread thread1 = createConversationThread(now.minusMinutes(1), "mark-conversation-1");
        postBoxRepository.upsertThread(postBoxId, thread1, true);
        AbstractConversationThread thread2 = createConversationThread(now.minusMinutes(1), "mark-conversation-2");

        PostBox box = new PostBox<>(email, Optional.empty(), Arrays.asList(thread1, thread2));
        postBoxRepository.write(box);

        PostBox postBox = postBoxRepository.byId(postBoxId);
        assertEquals(2, postBox.getConversationThreads().size());

        assertTrue(postBoxRepository.threadById(postBoxId, "mark-conversation-1").get().isContainsUnreadMessages());
        assertFalse(postBoxRepository.threadById(postBoxId, "mark-conversation-2").get().isContainsUnreadMessages());

        postBoxRepository.markConversationsAsRead(postBox, postBox.getConversationThreads());
        assertFalse(postBoxRepository.threadById(postBoxId, "mark-conversation-1").get().isContainsUnreadMessages());
        assertFalse(postBoxRepository.threadById(postBoxId, "mark-conversation-2").get().isContainsUnreadMessages());
        assertFalse(((AbstractConversationThread) postBox.lookupConversation("mark-conversation-1").get()).isContainsUnreadMessages());
        assertFalse(((AbstractConversationThread) postBox.lookupConversation("mark-conversation-2").get()).isContainsUnreadMessages());
    }

    @Test
    public void markConversationAsReadPostBoxModificationDate() throws Exception {
        String email = "markconversation@bar.com";
        DateTime oldModified = now();
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread thread1 = createConversationThread(oldModified.minusMinutes(10), "mark-conversation-modify-1");
        AbstractConversationThread thread2 = createConversationThread(oldModified.minusMinutes(10), "mark-conversation-modify-2");

        PostBox box = new PostBox<>(email, Optional.empty(), Arrays.asList(thread1, thread2));
        postBoxRepository.write(box);

        postBoxRepository.markConversationsAsRead(box, box.getConversationThreads());

        thread1 = postBoxRepository.threadById(postBoxId, "mark-conversation-modify-1").get();
        thread2 = postBoxRepository.threadById(postBoxId, "mark-conversation-modify-2").get();

        assertTrue(thread1.getModifiedAt().isAfter(oldModified));
        assertTrue(thread2.getModifiedAt().isAfter(oldModified));
    }

    @Test
    public void markConversationAsReadSingleThreadModificationDate() throws Exception {
        String email = "markconversation@bar.com";
        DateTime oldModified = now();
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread thread1 = createConversationThread(oldModified.minusMinutes(10), "mark-conversation-modify-1");
        AbstractConversationThread thread2 = createConversationThread(oldModified.minusMinutes(10), "mark-conversation-modify-2");

        PostBox box = new PostBox<>(email, Optional.empty(), Arrays.asList(thread1, thread2));
        postBoxRepository.write(box);

        postBoxRepository.markConversationAsRead(box, thread2);

        thread1 = postBoxRepository.threadById(postBoxId, "mark-conversation-modify-1").get();
        thread2 = postBoxRepository.threadById(postBoxId, "mark-conversation-modify-2").get();

        assertFalse(thread1.getModifiedAt().isAfter(oldModified));
        assertTrue(thread2.getModifiedAt().isAfter(oldModified));
    }

    @Test
    public void markConversationAsReadSorting() throws Exception {
        String email = "markconversationsorting@bar.com";
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        AbstractConversationThread thread1 = createConversationThread(now().minusDays(1), "mark-conversation-1");
        postBoxRepository.upsertThread(postBoxId, thread1, true);
        AbstractConversationThread thread2 = createConversationThread(now().minusDays(2), "mark-conversation-2");
        AbstractConversationThread thread3 = createConversationThread(now().minusDays(3), "mark-conversation-3");
        AbstractConversationThread thread4 = createConversationThread(now().minusDays(4), "mark-conversation-4");
        postBoxRepository.upsertThread(postBoxId, thread4, true);
        postBoxRepository.upsertThread(postBoxId, thread4, true);
        AbstractConversationThread thread5 = createConversationThread(now().minusDays(5), "mark-conversation-5");

        List<AbstractConversationThread> conversations = Arrays.asList(thread1, thread2, thread3, thread4, thread5);

        PostBox box = new PostBox<>(email, new Counter(3), conversations);
        postBoxRepository.write(box);

        PostBox<AbstractConversationThread> postBox = postBoxRepository.byId(postBoxId);
        postBoxRepository.markConversationsAsRead(postBox, conversations);

        assertEquals(5, postBox.getConversationThreads().size());
        assertEquals("mark-conversation-1", postBox.getConversationThreads().get(0).getConversationId());
        assertEquals("mark-conversation-2", postBox.getConversationThreads().get(1).getConversationId());
        assertEquals("mark-conversation-3", postBox.getConversationThreads().get(2).getConversationId());
        assertEquals("mark-conversation-4", postBox.getConversationThreads().get(3).getConversationId());
        assertEquals("mark-conversation-5", postBox.getConversationThreads().get(4).getConversationId());
    }

    @Test
    public void cleansUpPostbox() {
        PostBox box = new PostBox<>("foo@bar.com", Optional.empty(), Collections.singletonList(createConversationThread(now(), "123")));

        postBoxRepository.write(box);

        assertThat(postBoxRepository.cleanup(now())).isTrue();

        assertEquals("PostBox contains no conversation threads anymore", 0, postBoxRepository.byId(PostBoxId.fromEmail("foo@bar.com")).getConversationThreads().size());
    }

    @Test
    public void keepsPostboxEntriesThatAreTooNew() {
        PostBox box = new PostBox<>("foo@bar.com", Optional.empty(), Collections.singletonList(createConversationThread(now(), "123")));

        postBoxRepository.write(box);

        assertThat(postBoxRepository.cleanup(now().minusHours(1))).isTrue();

        assertEquals("PostBox still contains its 1 conversation thread", 1, postBoxRepository.byId(PostBoxId.fromEmail("foo@bar.com")).getConversationThreads().size());
    }

    @Test
    public void writesThread() throws Exception {
        String email = "foo@bar.com";
        AbstractConversationThread convThread = createConversationThread(now(), "123");

        ((CassandraSimpleMessageCenterRepository) postBoxRepository).writeThread(PostBoxId.fromEmail(email), convThread);

        PostBox box = new PostBox<>(email, Optional.empty(), Collections.singletonList(convThread));
        assertEquals("PostBox's conversationThread is updated", box, postBoxRepository.byId(PostBoxId.fromEmail(email)));
    }

    @Test
    public void writesThreadWithUnreadMessages() throws Exception {
        String email = "foo@bar.com";
        AbstractConversationThread convThread = createConversationThread(now(), "123");
        PostBoxId postBoxId = PostBoxId.fromEmail(email);

        ((CassandraSimpleMessageCenterRepository) postBoxRepository).writeThread(postBoxId, convThread);
        postBoxRepository.upsertThread(postBoxId, convThread, true);
        postBoxRepository.upsertThread(postBoxId, convThread, true);
        ((CassandraSimpleMessageCenterRepository) postBoxRepository).writeThread(postBoxId, convThread);

        assertEquals("Conversation thread should contain 2 unread messages", 2, postBoxRepository.unreadCountInConversation(postBoxId, convThread.getConversationId()));
    }

    @Test
    public void upsertsThread() throws Exception {
        String email = "foo@bar.com";
        DateTime now = now();
        AbstractConversationThread convThread = createConversationThread(now.minusMinutes(1), "123");
        AbstractConversationThread updatedConvThread = createConversationThread(now, "123");

        PostBox box = new PostBox<>(email, Optional.empty(), Collections.singletonList(convThread));
        postBoxRepository.write(box);

        postBoxRepository.upsertThread(PostBoxId.fromEmail(email), updatedConvThread, true);

        assertEquals("PostBox's conversationThread is updated", now.getMillis(), postBoxRepository.byId(PostBoxId.fromEmail(email)).getLastModification().getMillis());
    }

    private AbstractConversationThread createConversationThread(DateTime date, String conversationId) {
        return new ConversationThread("123", conversationId, date, date, date, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    @Configuration
    @Import({
      CassandraSimpleMessageCenterRepositoryConfiguration.class
    })
    static class TestContext {

        @Value("${persistence.cassandra.consistency.read:#{null}}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:#{null}}")
        private ConsistencyLevel cassandraWriteConsistency;

        @Bean
        public Session cassandraSessionForMb() {
            String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
            String[] schemas = new String[] { "cassandra_schema.cql", "cassandra_messagecenter_schema.cql" };

            return CassandraIntegrationTestProvisioner.getInstance().loadSchema(keyspace, schemas);
        }

        @Bean(name = "cassandraReadConsistency")
        public ConsistencyLevel getCassandraReadConsistency() {
            return cassandraReadConsistency;
        }

        @Bean(name = "cassandraWriteConsistency")
        public ConsistencyLevel getCassandraWriteConsistency() {
            return cassandraWriteConsistency;
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}