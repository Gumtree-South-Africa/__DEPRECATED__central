package com.ecg.messagecenter.ebayk.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.BasicVClock;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.RiakGzipAwareContentFilter;
import com.ecg.messagecenter.persistence.simple.RiakSimplePostBoxConverter;
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

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RiakSimplePostBoxConverterTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = riak"
})
public class RiakSimplePostBoxConverterTest {
    private static final String RAW_RIAK_JSON = "{\"version\":1,\"threads\":["
            + "{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},"
            + "{\"adId\":\"213\",\"createdAt\":1391368984880,\"modifiedAt\":1391368984880,\"conversationId\":\"bca\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984880},"
            + "{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}"
            + "]}\n";

    @Autowired
    private RiakSimplePostBoxConverter converter;

    @Test
    public void constructDeconstructPostboxPlainString() {
        PostBox<ConversationThread> postBox = createPostBox();

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        riakObject.setValue(RAW_RIAK_JSON);

        PostBox<ConversationThread> converted = converter.toDomain(riakObject);

        assertThat(converted.getConversationThreads()).isEqualTo(postBox.getConversationThreads());
        assertThat(converted.getEmail()).isEqualTo(postBox.getEmail());
        // WARNING: normal assert equals does not work any more due to the timestamp that is changed when creating a new post box
    }

    @Test
    public void constructDeconstructPostboxGzipped(){
        PostBox<ConversationThread> postBox = createPostBox();

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        riakObject.setValue(RiakGzipAwareContentFilter.compress(RAW_RIAK_JSON));

        PostBox<ConversationThread> converted = converter.toDomain(riakObject);

        assertThat(converted.getConversationThreads()).isEqualTo(postBox.getConversationThreads());
        assertThat(converted.getEmail()).isEqualTo(postBox.getEmail());
    }

    private PostBox<ConversationThread> createPostBox() {
        DateTime createdAt1 = new DateTime(1391368984813L);
        DateTime createdAt2 = new DateTime(1391368984879L);
        DateTime createdAt3 = new DateTime(1391368984880L);
        DateTime modifiedAt1 = new DateTime(1391368984875L);
        DateTime modifiedAt2 = new DateTime(1391368984879L);
        DateTime modifiedAt3 = new DateTime(1391368984880L);
        DateTime receivedAt1 = new DateTime(1391365384875L);
        DateTime receivedAt2 = new DateTime(1391332984879L);
        DateTime receivedAt3 = new DateTime(1391332984880L);

        return new PostBox<>("bla@blah.com", Optional.of(1L), Arrays.asList(
            new ConversationThread("123", "abc", createdAt1, modifiedAt1, receivedAt1, true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("321", "cba", createdAt2, modifiedAt2, receivedAt2, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("213", "bca", createdAt3, modifiedAt3, receivedAt3, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
        ));
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
