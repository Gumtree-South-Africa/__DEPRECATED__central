package com.ecg.messagecenter.persistence;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.http.util.Constants;
import com.ecg.messagecenter.persistence.block.*;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RiakConversationBlockRepositoryTest.TestContext.class})
@TestPropertySource(properties = {
        "persistence.strategy = riak",
        "replyts.maxConversationAgeDays = 25"
})
public class RiakConversationBlockRepositoryTest {
    public static final String CONV_ID = "convId";

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private ConversationBlockRepository repository;

    @Test
    public void persistsConversationBlock() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));
        ConversationBlock conversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        repository.write(conversationBlock);

        IRiakObject fetchedObj = riakClient.fetchBucket(RiakConversationBlockRepository.BUCKET_NAME).execute().fetch(CONV_ID).execute();
        assertNotNull(fetchedObj);
        assertEquals(CONV_ID, fetchedObj.getKey());
        assertEquals(Constants.CTYPE_JSON_UTF8, fetchedObj.getContentType());
    }

    @Test
    public void cleansUpConversationBlocks() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));
        ConversationBlock conversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        repository.write(conversationBlock);

        repository.cleanup(now.get().plusSeconds(1));

        IRiakObject fetchedObj = riakClient.fetchBucket(RiakConversationBlockRepository.BUCKET_NAME).execute().fetch(CONV_ID).execute();

        assertNull(fetchedObj);
    }

    @Test
    public void doesntCleanUpRecentConversationBlocks() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));
        ConversationBlock conversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        repository.write(conversationBlock);

        repository.cleanup(now.get().minusSeconds(10));

        IRiakObject fetchedObj = riakClient.fetchBucket(RiakConversationBlockRepository.BUCKET_NAME).execute().fetch(CONV_ID).execute();

        assertNotNull(fetchedObj);
        assertEquals(CONV_ID, fetchedObj.getKey());
    }

    @Configuration
    @Import({RiakConversationBlockConfiguration.class, RiakConversationBlockConfiguration.class, JsonToConversationBlockConverter.class, ConversationBlockToJsonConverter.class})
    static class TestContext {
        @Bean
        public IRiakClient riakClient() {
            return new EmbeddedRiakClient();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}
