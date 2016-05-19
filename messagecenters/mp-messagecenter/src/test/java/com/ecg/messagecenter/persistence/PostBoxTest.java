package com.ecg.messagecenter.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertTrue;

public class PostBoxTest {

    public static final PostBox POST_BOX = new PostBox("" +
            "bla@blah.com",
            Lists.newArrayList(
                    new ConversationThread("2", "abc", now(), now().minusDays(179), now(),
                            1, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                            Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                    new ConversationThread("3", "abc", now(), now().minusDays(100), now(),
                            1, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                            Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                    new ConversationThread("4", "abc", now(), now().minusHours(100), now(),
                            1, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                            Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                    new ConversationThread("5", "abc", now(), now().minusSeconds(100), now(),
                            1, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                            Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                    new ConversationThread("6", "cba", now(), now().minusHours(10), now().minusHours(4),
                            1, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                            Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now().minusHours(4)))
            )
    );

    @Test
    public void sortsByModificationDate() {
        PostBox postBox = new PostBox("" +
                "bla@blah.com",
                Lists.newArrayList(
                        new ConversationThread("123", "abc", now(), now().minusHours(100), now(), 1,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                        new ConversationThread("321", "cba", now(), now().minusHours(10), now().minusHours(4), 0,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now().minusHours(4)))
                )
        );

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertEquals("123", conversationThreads.get(0).getAdId());
        assertTrue(conversationThreads.size() == 2);
    }

    @Test
    public void marksConversationAsUnread() {
        PostBox postBox = new PostBox("bla@blah.com", Lists.newArrayList(
                new ConversationThread("adid1", "convid2", now().minusDays(4), now(), now(), 0, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now()))
        ));

        postBox.markConversationUnread("convid2", null);
        assertEquals(1, postBox.getNewRepliesCounter());
        assertTrue(postBox.getUnreadConversations().containsKey("convid2"));
    }

    @Test
    public void removeOldConversations() {
        PostBox pb = new PostBox(
                "bla@blah.com",
                Lists.newArrayList(
                        new ConversationThread("1", "abc", now().minusDays(180).minusSeconds(5), now(), now(), 1,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                        new ConversationThread("2", "abc", now().minusDays(180).plusSeconds(5), now(), now().minusHours(1), 1,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                        new ConversationThread("3", "abc", now().minusDays(100), now(), now().minusHours(2), 1,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                        new ConversationThread("4", "abc", now().minusHours(100), now(), now().minusHours(3), 1,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                        new ConversationThread("5", "abc", now().minusSeconds(100), now(), now().minusHours(4), 1,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now())),
                        new ConversationThread("6", "cba", now().minusHours(10), now(), now().minusHours(5), 0,
                                Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(),
                                Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now()))
                )
        );

        List<ConversationThread> conversationThreads = pb.getConversationThreads();

        List<String> containsIds = Lists.newArrayList("2", "3", "4", "5", "6");

        conversationThreads.forEach(x -> assertTrue(containsIds.contains(x.getAdId())));
    }

    @Test
    public void removeNoConversations() {
        PostBox postBox = POST_BOX;

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertTrue(conversationThreads.size() == 5);
    }

    @Test
    public void cappingForPageZeroReturnsFirstResults() {
        List<ConversationThread> conversationThreadsCapTo = POST_BOX.getConversationThreadsCapTo(0, 2);

        assertEquals(POST_BOX.getConversationThreads().subList(0, 2), conversationThreadsCapTo);
    }

    @Test
    public void cappingForPage1ReturnsNextResults() {
        List<ConversationThread> conversationThreadsCapTo = POST_BOX.getConversationThreadsCapTo(1, 2);

        assertEquals(POST_BOX.getConversationThreads().subList(2, 4), conversationThreadsCapTo);
    }

    @Test
    public void sortConversationThreads() {
        // sort based on last message created date if present
        PostBox postBox1 = new PostBox("userId1", Lists.newArrayList(
                new ConversationThread("adId1", "convId1", now().minusDays(4), now(), now(), 0, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now().minusMinutes(4))),
                new ConversationThread("adId2", "convId2", now().minusDays(4), now(), now(), 0, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(now().minusMinutes(2)))));

        assertEquals("convId2", postBox1.getConversationThreads().get(0).getConversationId());
        assertEquals("convId1", postBox1.getConversationThreads().get(1).getConversationId());

        // sort based on conversation received date, if last message create date not present
        PostBox postBox2 = new PostBox("userId2", Lists.newArrayList(
                new ConversationThread("adId3", "convId3", now().minusDays(4), now(), now().minusMinutes(4), 0, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<DateTime>absent()),
                new ConversationThread("adId4", "convId4", now().minusDays(4), now(), now().minusMinutes(2), 0, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<DateTime>absent())));

        assertEquals("convId4", postBox2.getConversationThreads().get(0).getConversationId());
        assertEquals("convId3", postBox2.getConversationThreads().get(1).getConversationId());
    }
}
