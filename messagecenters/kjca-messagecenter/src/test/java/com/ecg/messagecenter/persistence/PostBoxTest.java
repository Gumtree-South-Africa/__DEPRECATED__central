package com.ecg.messagecenter.persistence;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertTrue;

public class PostBoxTest {

    public static final PostBox POST_BOX = new PostBox("" +
            "bla@blah.com",
            Optional.of(0L),
            Lists.newArrayList(
                    new ConversationThread("2", "abc",now(UTC), now(UTC).minusDays(179), now(UTC), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("3", "abc",now(UTC), now(UTC).minusDays(100), now(UTC), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("4", "abc",now(UTC), now(UTC).minusHours(100), now(UTC), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("5", "abc",now(UTC), now(UTC).minusSeconds(100), now(UTC), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("6", "cba",now(UTC), now(UTC).minusHours(10), now(UTC).minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
            )
    );

    @Test
    public void sortsByModificationDate() {
        PostBox postBox = new PostBox("" +
                "bla@blah.com",
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("123", "abc", now(UTC), now(UTC).minusHours(100), now(UTC), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("321", "cba",now(UTC),  now(UTC).minusHours(10), now(UTC).minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                )
        );

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertEquals("123", conversationThreads.get(0).getAdId());
        assertTrue(conversationThreads.size() == 2);
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
