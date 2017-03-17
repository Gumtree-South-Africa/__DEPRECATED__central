package com.ecg.de.ebayk.messagecenter.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertTrue;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:48
 *
 * @author maldana@ebay.de
 */
public class PostBoxTest {

    public static final PostBox POST_BOX = new PostBox.PostBoxBuilder().withEmail("bla@blah.com").
            withNewRepliesCounter(0L).
            withConversationThreads(Lists.newArrayList(
                new ConversationThread("2", "abc", now(), now().minusDays(179), now(), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("3", "abc", now(), now().minusDays(100), now(), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("4", "abc", now(), now().minusHours(100), now(), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("5", "abc", now(), now().minusSeconds(100), now(), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("6", "cba", now(), now().minusHours(10), now().minusHours(4), false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent())
    )).build();

    @Test
    public void sortsByModificationDate() {
        PostBox postBox = new PostBox.PostBoxBuilder().withEmail("" +
                "bla@blah.com").withNewRepliesCounter(0L).withConversationThreads(Lists.newArrayList(
                new ConversationThread("123", "abc", now(), now().minusHours(100), now(), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("321", "cba", now(), now().minusHours(10), now().minusHours(4), false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent())
        )).build();

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertEquals("123", conversationThreads.get(0).getAdId());
        assertTrue(conversationThreads.size() == 2);
    }

    @Test
    public void removeOldConversations() {

        PostBox pb = new PostBox.PostBoxBuilder().withEmail("bla@blah.com").withNewRepliesCounter(0L).withMaxConversationAgeDays(10).withConversationThreads(Lists.newArrayList(
                new ConversationThread("1", "abc", now().minusDays(30), now(), now(), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("2", "abc", now().minusDays(29), now(), now().minusHours(2), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("3", "abc", now().minusDays(10).plusSeconds(1), now(), now().minusHours(1), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("4", "conv7", now().minusDays(9), now(), now().minusHours(5), false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("5", "abc", now().minusHours(29), now(), now().minusHours(3), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("6", "abc", now().minusSeconds(29), now(), now().minusHours(4), true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent()),
                new ConversationThread("7", "cba", now().minusHours(2), now().minusHours(3), now().minusHours(3), false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent())
        )).build();

        List<ConversationThread> conversationThreads = pb.getConversationThreads();

        assertTrue(conversationThreads.size() == 5);
        Set<String> convThreadIds = ImmutableSet.of("3", "4", "5", "6", "7");
        conversationThreads.forEach(convThread -> assertTrue(convThreadIds.contains(convThread.getAdId())));
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
}
