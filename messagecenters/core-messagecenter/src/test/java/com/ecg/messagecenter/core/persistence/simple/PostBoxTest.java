package com.ecg.messagecenter.core.persistence.simple;

import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertTrue;

public class PostBoxTest {
    public static final PostBox POST_BOX = new PostBox("" +
            "bla@blah.com",
            Optional.of(0L),
            Lists.newArrayList(
                    new ConversationThread("2", "abc", now(), now().minusDays(179), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("3", "abc", now(), now().minusDays(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("4", "abc", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("5", "abc", now(), now().minusSeconds(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                    new ConversationThread("6", "cba", now(), now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
            )
    );

    @Test
    public void sortsByModificationDate() {
        PostBox postBox = new PostBox("" +
                "bla@blah.com",
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("123", "abc", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                        new ConversationThread("321", "cba", now(), now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
                )
        );

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertEquals("123", conversationThreads.get(0).getAdId());
        assertTrue(conversationThreads.size() == 2);
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

    @Test
    public void filterByRole() throws Exception {
        PostBox postBox = new PostBox("" +
                "buyer@blah.com",
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("123", "abc", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("seller@blah.com"), Optional.empty()),
                        new ConversationThread("321", "cba", now(), now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("buyer@blah.com"), Optional.empty())
                )
        );
        Predicate<AbstractConversationThread> buyerFilter = conversation -> conversation.getBuyerId().get().equals("buyer@blah.com");
        //I know this is confusing, just following the logic of ConversationBoundnessFinder and the main purpose is to make sure the predicate works as expected
        Predicate<AbstractConversationThread> sellerFilter = conversation -> conversation.getBuyerId().get().equals("seller@blah.com");
        List<ConversationThread> buyerConversations = postBox.getFilteredConversationThreads(buyerFilter, 0, 10);
        List<ConversationThread> sellerConversations = postBox.getFilteredConversationThreads(sellerFilter, 0, 10);
        assertEquals(1, buyerConversations.size());
        assertEquals(1, sellerConversations.size());
        assertEquals("321", buyerConversations.get(0).getAdId());
        assertEquals("123", sellerConversations.get(0).getAdId());

    }

    public static AbstractConversationThread createConversationThread(DateTime createdAt, DateTime modifiedAt, String conversationId) {
        DateTime receivedDate = modifiedAt;

        return new ConversationThread("123", conversationId, createdAt, modifiedAt, receivedDate, false, Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty());
    }
}
