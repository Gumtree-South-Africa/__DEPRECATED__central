package com.ecg.messagecenter.persistence;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;

public class PostBoxTest {

    private static final List<ConversationThread> CONVERSATION_THREADS = Lists.newArrayList(
            new ConversationThread("2", "abc", now(), now().minusDays(179), now(),
                    1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(now())),
            new ConversationThread("3", "abc", now(), now().minusDays(100), now(),
                    1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(now())),
            new ConversationThread("4", "abc", now(), now().minusHours(100), now(),
                    1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(now())),
            new ConversationThread("5", "abc", now(), now().minusSeconds(100), now(),
                    1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(now())),
            new ConversationThread("6", "cba", now(), now().minusHours(10), now().minusHours(4),
                    1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(now().minusHours(4))));

    private static final PostBox POST_BOX = new PostBox("bla@blah.com", CONVERSATION_THREADS);

    @Test
    public void cappingForPageZeroReturnsFirstResults() {
        List<ConversationThread> conversationThreadsCapTo = POST_BOX.getConversationThreadsCapTo(0, 2);

        assertEquals(CONVERSATION_THREADS.subList(0, 2), conversationThreadsCapTo);
    }

    @Test
    public void cappingForPage1ReturnsNextResults() {
        List<ConversationThread> conversationThreadsCapTo = POST_BOX.getConversationThreadsCapTo(1, 2);

        assertEquals(CONVERSATION_THREADS.subList(2, 4), conversationThreadsCapTo);
    }

    @Test
    public void sortConversationThreads() {
        // sort based on last message created date if present
        PostBox postBox1 = new PostBox("userId1", Lists.newArrayList(
                new ConversationThread("adId1", "convId1", now().minusDays(4), now(), now(), 0, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.of(now().minusMinutes(4))),
                new ConversationThread("adId2", "convId2", now().minusDays(4), now(), now(), 0, Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.of(now().minusMinutes(2)))));

        assertEquals("convId2", postBox1.getConversationThreads().get(0).getConversationId());
        assertEquals("convId1", postBox1.getConversationThreads().get(1).getConversationId());

        // sort based on conversation received date, if last message create date not present
        PostBox postBox2 = new PostBox("userId2", Lists.newArrayList(
                new ConversationThread("adId3", "convId3", now().minusDays(4), now(), now().minusMinutes(4), 0,
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()),
                new ConversationThread("adId4", "convId4", now().minusDays(4), now(), now().minusMinutes(2), 0,
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty())));

        assertEquals("convId4", postBox2.getConversationThreads().get(0).getConversationId());
        assertEquals("convId3", postBox2.getConversationThreads().get(1).getConversationId());
    }
}