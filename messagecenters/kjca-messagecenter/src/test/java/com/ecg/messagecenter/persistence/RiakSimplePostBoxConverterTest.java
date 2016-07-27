package com.ecg.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.BasicVClock;
import com.ecg.messagecenter.persistence.simple.*;
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

import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RiakSimplePostBoxConverterTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = riak",
  "replyts.maxConversationAgeDays = 25"
})
public class RiakSimplePostBoxConverterTest {
    @Autowired
    private RiakSimplePostBoxConverter converter;

    @Test
    public void constructDeconstructPostboxPlainString() {
        PostBox postBox = createPostBox();

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        // overwriting value with string json old style
        riakObject.setValue("{\"version\":1,\"threads\":[{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}]}\n");

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
        // WARNING: normal assert equals does not work any more due to the timestamp that is changed when creating a new post box
    }

    @Test
    public void constructDeconstructPostboxGzipped(){
        PostBox postBox = createPostBox();

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        riakObject.setValue(RiakGzipAwareContentFilter.compress("{\"version\":1,\"threads\":[{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}]}\n"));
        riakObject.setContentType("application/x-gzip");

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
    }

    @Test
    public void receivedAtIsNullSafeDeconstruct(){
        PostBox postBox = new PostBox("bla@blah.com", Optional.of(1l), Lists.newArrayList(
                new ConversationThread("321", "cba", now(UTC), now(UTC), null, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())), 180);

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
    }

    private PostBox createPostBox() {
        DateTime dt = new DateTime(UTC);
        DateTime createdAt1 = dt.withMillis(1391368984813L);
        DateTime createdAt2 = dt.withMillis(1391368984879L);
        DateTime modifiedAt1 = dt.withMillis(1391368984875L);
        DateTime modifiedAt2 = dt.withMillis(1391368984879L);
        DateTime receivedAt1 = dt.withMillis(1391365384875L);
        DateTime receivedAt2 = dt.withMillis(1391332984879L);

        return new PostBox("bla@blah.com", Optional.of(1L), Lists.newArrayList(
          new ConversationThread("123", "abc", createdAt1, modifiedAt1, receivedAt1, true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
          new ConversationThread("321", "cba", createdAt2, modifiedAt2, receivedAt2, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
        ), 180);
    }

    @Configuration
    @Import({ JsonToPostBoxConverter.class, PostBoxToJsonConverter.class })
    static class TestContext {
        @Bean
        public RiakSimplePostBoxConverter converter() {
            return new RiakSimplePostBoxConverter();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}
