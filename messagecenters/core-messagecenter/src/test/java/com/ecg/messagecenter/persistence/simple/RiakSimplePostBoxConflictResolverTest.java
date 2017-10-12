package com.ecg.messagecenter.persistence.simple;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static com.ecg.messagecenter.persistence.simple.PostBoxTest.createConversationThread;
import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RiakSimplePostBoxConflictResolverTest.TestContext.class)
@TestPropertySource(properties = {
  "replyts.maxConversationAgeDays = 180"
})
public class RiakSimplePostBoxConflictResolverTest {
    private static final DateTime CREATED_AT = now();

    @Autowired
    private RiakSimplePostBoxConflictResolver resolver;

    @Test
    public void disjunctPostBoxes() {
        DateTime twoHoursAgo = now().minusHours(2);

        PostBox postBox1 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConversationThread(CREATED_AT, twoHoursAgo, "a:1")));

        DateTime threeHoursAgo = now().minusHours(5);
        PostBox postBox2 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConversationThread(CREATED_AT, threeHoursAgo, "b:2")));

        PostBox resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(
            createConversationThread(CREATED_AT, twoHoursAgo, "a:1"),
            createConversationThread(CREATED_AT, threeHoursAgo, "b:2")
          ));

        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void usesLatestThreadVersionWhenInFirstThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);

        PostBox postBox1 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConversationThread(CREATED_AT, twoHoursAgo, "a:1")));
        PostBox postBox2 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConversationThread(CREATED_AT, threeHoursAgo, "a:1")));

        PostBox resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConversationThread(CREATED_AT, twoHoursAgo, "a:1")));

        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void usesLatestThreadVersionWhenInSecondThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);

        PostBox postBox1 = new PostBox(
         "foo@bar.de",
         Optional.of(3L),
         Lists.newArrayList(createConversationThread(CREATED_AT, threeHoursAgo, "a:1")));
        PostBox postBox2 = new PostBox(
          "foo@bar.de",
          Optional.of(2L),
          Lists.newArrayList(createConversationThread(CREATED_AT, twoHoursAgo, "a:1")));

        PostBox resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
          "foo@bar.de",
          Optional.of(3L),
          Lists.newArrayList(createConversationThread(CREATED_AT, twoHoursAgo, "a:1")));

        assertEquals(expected, resolvedPostBox);
    }

    @Configuration
    static class TestContext {
        @Bean
        public RiakSimplePostBoxMerger merger() {
            return new RiakSimplePostBoxMerger();
        }

        @Bean
        public RiakSimplePostBoxConflictResolver resolver() {
            return new RiakSimplePostBoxConflictResolver();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}
