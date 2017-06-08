package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.PostBox;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:36
 *
 * @author maldana@ebay.de
 */
public class PostBoxConflictResolverTest {


    private static final DateTime CREATED_AT = now();
    private PostBoxConflictResolver resolver;
    private int maxAgeDays = 180;

    @Before public void setUp() {
        resolver = new PostBoxConflictResolver(maxAgeDays);
    }

    @Test public void disjunctPostBoxes() {
        DateTime twoHoursAgo = now().minusHours(2);

        PostBox<ConversationThread> postBox1 = new PostBox<>("foo@bar.de", Optional.of(1L), Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")), maxAgeDays);
        DateTime threeHoursAgo = now().minusHours(5);
        PostBox<ConversationThread> postBox2 = new PostBox<>("foo@bar.de", Optional.of(1L), Lists.newArrayList(createConvThread(threeHoursAgo, "b:2")), maxAgeDays);

        PostBox<ConversationThread> resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox<ConversationThread> expected = new PostBox<>("foo@bar.de", Optional.of(1L), Lists.newArrayList(createConvThread(twoHoursAgo, "a:1"),
                createConvThread(threeHoursAgo, "b:2")), maxAgeDays);
        assertEquals(expected, resolvedPostBox);
    }


    @Test public void usesLatestThreadVersionWhenInFirstThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);


        PostBox<ConversationThread> postBox1 = new PostBox<>("foo@bar.de", Optional.of(1L), Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")), maxAgeDays);
        PostBox<ConversationThread> postBox2 = new PostBox<>("foo@bar.de", Optional.of(1L), Lists.newArrayList(createConvThread(threeHoursAgo, "a:1")), maxAgeDays);

        PostBox<ConversationThread> resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox<ConversationThread> expected = new PostBox<>("foo@bar.de", Optional.of(1L), Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")), maxAgeDays);
        assertEquals(expected, resolvedPostBox);
    }

    @Test public void usesLatestThreadVersionWhenInSecondThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);


        PostBox<ConversationThread> postBox1 = new PostBox<>("foo@bar.de", Optional.of(3L), Lists.newArrayList(createConvThread(threeHoursAgo, "a:1")), maxAgeDays);
        PostBox<ConversationThread> postBox2 = new PostBox<>("foo@bar.de", Optional.of(2L), Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")), maxAgeDays);

        PostBox<ConversationThread> resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox<ConversationThread> expected = new PostBox<>("foo@bar.de", Optional.of(3L), Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")), maxAgeDays);

        assertEquals(expected, resolvedPostBox);
    }

    private ConversationThread createConvThread(DateTime modifiedAt, String convId) {
        DateTime receivedDate = modifiedAt;
        return new ConversationThread("123", convId, CREATED_AT, modifiedAt, receivedDate, false,
                        Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(),
                        Optional.empty());

    }
}
