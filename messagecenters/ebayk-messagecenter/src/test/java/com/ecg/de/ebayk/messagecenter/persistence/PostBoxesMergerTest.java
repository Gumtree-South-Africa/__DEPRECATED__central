package com.ecg.de.ebayk.messagecenter.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;

public class PostBoxesMergerTest {


    private static final DateTime CREATED_AT = now();
    private PostBoxesMerger merger;

    @Before
    public void setUp() {
        merger = new PostBoxesMerger();
    }

    @Test
    public void disjunctPostBoxes() {
        DateTime twoHoursAgo = now().minusHours(2);

        PostBox postBox1 = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(twoHoursAgo, "a:1")
                ));
        DateTime threeHoursAgo = now().minusHours(5);
        PostBox postBox2 = new PostBox(
                "foo@bar.de",
                Optional.of(1L),
                Lists.newArrayList(
                        createConvThread(threeHoursAgo, "b:2")));

        PostBox resolvedPostBox = merger.merge(Lists.newArrayList(postBox1, postBox2));

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
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);


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
                Lists.newArrayList(
                        createConvThread(threeHoursAgo, "a:1")
                ));
        PostBox postBox2 = new PostBox(
                "foo@bar.de",
                Optional.of(2L),
                Lists.newArrayList(
                        createConvThread(twoHoursAgo, "a:1"))
        );

        PostBox resolvedPostBox = merger.merge(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = new PostBox(
                "foo@bar.de",
                Optional.of(3L),
                Lists.newArrayList(createConvThread(twoHoursAgo, "a:1"))
        );

        assertEquals(expected, resolvedPostBox);
    }

    private ConversationThread createConvThread(DateTime modifiedAt, String convId) {
        DateTime receivedDate = modifiedAt;
        return new ConversationThread("123", convId, CREATED_AT, modifiedAt, receivedDate, false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent());

    }

}