package com.ecg.messagecenter.persistence;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:36
 *
 * @author maldana@ebay.de
 */
public class PostBoxConflictResolverTest {


    private static final DateTime CREATED_AT = now(UTC);
    private PostBoxConflictResolver resolver;

    @Before
    public void setUp() {
        resolver = new PostBoxConflictResolver();
    }

    @Test
    public void disjunctPostBoxes() {
        DateTime twoHoursAgo = now(UTC).minusHours(2);

        PostBox postBox1 = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(twoHoursAgo, "a:1")
                ));
        DateTime threeHoursAgo = now(UTC).minusHours(5);
        PostBox postBox2 = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(threeHoursAgo, "b:2")));

        PostBox resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(twoHoursAgo, "a:1"),
                        createConvThread(threeHoursAgo, "b:2")));
        assertEquals(expected, resolvedPostBox);
    }


    @Test
    public void usesLatestThreadVersionWhenInFirstThread() {
        DateTime twoHoursAgo = now(UTC).minusHours(2);
        DateTime threeHoursAgo = now(UTC).minusHours(5);


        PostBox postBox1 = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(twoHoursAgo, "a:1")
                ));
        PostBox postBox2 = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(threeHoursAgo, "a:1")));

        PostBox resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(createConvThread(twoHoursAgo, "a:1")));
        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void usesLatestThreadVersionWhenInSecondThread() {
        DateTime twoHoursAgo = now(UTC).minusHours(2);
        DateTime threeHoursAgo = now(UTC).minusHours(5);


        PostBox postBox1 = new PostBox(
                "foo@bar.de",
                Optional.of(3L),
                Lists.newArrayList(
                        createConvThread(threeHoursAgo, "a:1")
                ));
        PostBox postBox2 = new PostBox(
                "foo@bar.de",
                Optional.of(2L),
                Lists.newArrayList(
                        createConvThread(twoHoursAgo, "a:1"))
        );

        PostBox resolvedPostBox = resolver.resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
                "foo@bar.de",
                Optional.of(3L),
                Lists.newArrayList(createConvThread(twoHoursAgo, "a:1"))
        );

        assertEquals(expected, resolvedPostBox);
    }

    private ConversationThread createConvThread(DateTime modifiedAt, String convId) {
        DateTime receivedDate = modifiedAt;
        return new ConversationThread("123", convId, CREATED_AT, modifiedAt, receivedDate, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    }
}
