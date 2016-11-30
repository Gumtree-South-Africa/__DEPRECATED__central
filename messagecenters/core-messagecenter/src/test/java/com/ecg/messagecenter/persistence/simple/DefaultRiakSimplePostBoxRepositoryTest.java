package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DefaultRiakSimplePostBoxRepositoryTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = riak",
  "replyts.maxConversationAgeDays = 25"
})
public class DefaultRiakSimplePostBoxRepositoryTest {
    private static final DateTime CREATED_AT = now();

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private DefaultRiakSimplePostBoxRepository postBoxRepository;

    @Test
    public void persistsPostbox() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<AbstractConversationThread>emptyList(), 25);
        postBoxRepository.write(box);

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNotNull(postbox);
        assertEquals("foo@bar.com", postbox.getKey());
        assertEquals("application/x-gzip", postbox.getContentType());
    }

    @Test
    public void cleansUpPostbox() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<AbstractConversationThread>emptyList(), 25);
        postBoxRepository.write(box);

        postBoxRepository.cleanup(now());

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNull(postbox);
    }

    @Test
    public void keepsPostboxEntriesThatAreTooNew() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<AbstractConversationThread>emptyList(), 25);
        postBoxRepository.write(box);

        postBoxRepository.cleanup(now().minusSeconds(1));

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNotNull(postbox);
    }

    @Test
    public void upsertThreadUnreadCount() {
        AbstractConversationThread thread = createConvThread(DateTime.now(), "a:1", true);

        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Lists.newArrayList(thread), 25);

        postBoxRepository.write(box);

        assertEquals(1L, (long) postBoxRepository.upsertThread("foo@bar.com", thread, true));
        assertEquals(1L, (long) postBoxRepository.byId("foo@bar.com").getUnreadConversations().size());

        // newRepliesCounter doesn't get persisted

        assertEquals(0L, (long) postBoxRepository.byId("foo@bar.com").getNewRepliesCounter().getValue());
    }

    private static AbstractConversationThread createConvThread(DateTime modifiedAt, String convId, boolean containsUnreadMessages) {
        DateTime receivedDate = modifiedAt;

        return new PostBoxTest.ConversationThread("123", convId, CREATED_AT, modifiedAt, receivedDate, containsUnreadMessages, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Configuration
    @Import({ RiakSimplePostBoxConfiguration.class })
    static class TestContext {
        @Bean
        public IRiakClient riakClient() {
            return new EmbeddedRiakClient();
        }

        @Bean
        public AbstractJsonToPostBoxConverter jsonToPostBoxConverter() {
            return new AbstractJsonToPostBoxConverter() {
                @Override
                public PostBox toPostBox(String key, String jsonString, int maxAgeDays) {
                    List<AbstractConversationThread> threads = new ArrayList<>();

                    for (String unreadLine : jsonString.split("\n")) {
                        String[] values = unreadLine.split("=");

                        threads.add(createConvThread(DateTime.now(), values[0], Boolean.valueOf(values[1])));
                    }

                    return new PostBox("foo@bar.com", Optional.empty(), threads, maxAgeDays);
                }
            };
        }

        @Bean
        public AbstractPostBoxToJsonConverter postBoxToJsonConverter() {
            return new AbstractPostBoxToJsonConverter() {
                @Override
                public String toJson(PostBox p) {
                    StringBuffer buffer = new StringBuffer();

                    final AtomicInteger index = new AtomicInteger();

                    // Only write the conversationId and hasUnreadMessages; we can then deserialize this again above
                    // so that it can be used to verify proper unread-conversation-persistence (json->pb and pb->json
                    // converters are only contained in the tenant-specific packages; so can't reuse those here)

                    for (AbstractConversationThread c : (List<AbstractConversationThread>) p.getConversationThreads()) {
                          buffer.append(c.getConversationId()).append("=").append(c.isContainsUnreadMessages()).append("\n");
                    }

                    return buffer.toString();
                }
            };
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}