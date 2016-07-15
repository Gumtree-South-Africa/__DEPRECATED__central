package com.ecg.messagecenter.persistence;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertTrue;

public class PostBoxTest {

    public static final PostBox POST_BOX = new PostBox("" +
            "bla@blah.com",
            Optional.of(0L),
            Lists.newArrayList(
                    new ConversationThread("2", "abc",now(), now().minusDays(179), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("3", "abc",now(), now().minusDays(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("4", "abc",now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("5", "abc",now(), now().minusSeconds(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("6", "cba",now(), now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
            )
    );

    @Test
    public void sortsByModificationDate() {
        PostBox postBox = new PostBox("" +
                "bla@blah.com",
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("123", "abc", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("321", "cba",now(),  now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                )
        );

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertEquals("123", conversationThreads.get(0).getAdId());
        assertTrue(conversationThreads.size() == 2);
    }


    @Test
    public void removeOldConversations() {

        PostBox pb =  new PostBox(
                "bla@blah.com",
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("1", "abc", now().minusDays(180), now(), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("2", "abc", now().minusDays(180).plusSeconds(1), now(), now().minusHours(1), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("3", "abc", now().minusDays(100), now(), now().minusHours(2), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("4", "abc", now().minusHours(100), now(), now().minusHours(3), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("5", "abc", now().minusSeconds(100), now(), now().minusHours(4), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("6", "cba", now().minusHours(10), now(), now().minusHours(5), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                )
        );


        List<ConversationThread> conversationThreads = pb.getConversationThreads();

        assertTrue(conversationThreads.size() == 5);
        assertEquals("2", conversationThreads.get(0).getAdId());
        assertEquals("3", conversationThreads.get(1).getAdId());
        assertEquals("4", conversationThreads.get(2).getAdId());
        assertEquals("5", conversationThreads.get(3).getAdId());
        assertEquals("6", conversationThreads.get(4).getAdId());
    }


    @Test
    public void removeNoConversations() {
        List<ConversationThread> conversationThreads = POST_BOX.getConversationThreads();

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
