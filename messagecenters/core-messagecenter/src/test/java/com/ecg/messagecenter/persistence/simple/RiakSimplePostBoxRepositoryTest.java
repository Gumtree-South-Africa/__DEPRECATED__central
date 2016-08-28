package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
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

import java.util.Collections;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RiakSimplePostBoxRepositoryTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = riak",
  "replyts.maxConversationAgeDays = 25"
})
public class RiakSimplePostBoxRepositoryTest {
    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private RiakSimplePostBoxRepository postBoxRepository;

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
                    return new PostBox("foo@bar.com", Optional.empty(), Collections.<AbstractConversationThread>emptyList(), maxAgeDays);
                }
            };
        }

        @Bean
        public AbstractPostBoxToJsonConverter postBoxToJsonConverter() {
            return new AbstractPostBoxToJsonConverter() {
                @Override
                public String toJson(PostBox p) {
                    return "{ \"foo\": \"bar\" }";
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