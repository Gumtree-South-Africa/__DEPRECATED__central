package com.ecg.messagecenter.core.persistence.simple;

import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.RiakSimplePostBoxMerger;
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

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RiakSimplePostBoxMergerTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = riak",
  "replyts.maxConversationAgeDays = 180"
})
public class RiakSimplePostBoxMergerTest {
    private static final DateTime CREATED_AT = now();

    @Autowired
    private RiakSimplePostBoxMerger merger;

    @Test
    public void disjunctPostBoxes() {
        DateTime twoHoursAgo = now().minusHours(2);
        PostBox postBox1 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")));

        DateTime threeHoursAgo = now().minusHours(5);
        PostBox postBox2 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConvThread(threeHoursAgo, "b:2")));

        PostBox resolvedPostBox = merger.merge(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(
            createConvThread(twoHoursAgo, "a:1"),
            createConvThread(threeHoursAgo, "b:2")
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
          Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")));

        PostBox postBox2 = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConvThread(threeHoursAgo, "a:1")));

        PostBox resolvedPostBox = merger.merge(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
          "foo@bar.de",
          Optional.of(1L),
          Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")));

        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void usesLatestThreadVersionWhenInSecondThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);

        PostBox postBox1 = new PostBox(
          "foo@bar.de",
          Optional.of(3L),
          Lists.newArrayList(createConvThread(threeHoursAgo, "a:1")));
        PostBox postBox2 = new PostBox(
          "foo@bar.de",
          Optional.of(2L),
          Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")));

        PostBox resolvedPostBox = merger.merge(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
          "foo@bar.de",
          Optional.of(3L),
          Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")));

        assertEquals(expected, resolvedPostBox);
    }

    private AbstractConversationThread createConvThread(DateTime modifiedAt, String convId) {
        DateTime receivedDate = modifiedAt;

        return new PostBoxTest.ConversationThread("123", convId, CREATED_AT, modifiedAt, receivedDate, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Configuration
    static class TestContext {
        @Bean
        public RiakSimplePostBoxMerger postBoxMerger() {
            return new RiakSimplePostBoxMerger();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}
