package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
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
      ),
      180
    );

    @Test
    public void sortsByModificationDate() {
        PostBox postBox = new PostBox("" +
          "bla@blah.com",
          Optional.of(0L),
          Lists.newArrayList(
            new ConversationThread("123", "abc", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("321", "cba", now(),  now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
          ),
          180
        );

        List<ConversationThread> conversationThreads = postBox.getConversationThreads();

        assertEquals("123", conversationThreads.get(0).getAdId());
        assertTrue(conversationThreads.size() == 2);
    }

    @Test
    public void marksConversationAsUnread() {
        PostBox postBox = new PostBox("bla@blah.com", Optional.of(0l), Lists.newArrayList(
          new ConversationThread("adid1", "convid2", now().minusDays(4), now(), now(), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
        ), 180);

        postBox.markConversationUnread("convid2", null);
        assertEquals(1l, postBox.getNewRepliesCounter().getValue().longValue());
        assertTrue(postBox.getUnreadConversations().containsKey("convid2"));
    }

    @Test
    public void removeOldConversations() {
        PostBox pb = new PostBox(
          "bla@blah.com",
          Optional.of(0L),
          Lists.newArrayList(
            new ConversationThread("1", "abc", now().minusDays(180), now(), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("2", "abc", now().minusDays(180).plusSeconds(1), now(), now().minusHours(1), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("3", "abc", now().minusDays(100), now(), now().minusHours(2), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("4", "abc", now().minusHours(100), now(), now().minusHours(3), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("5", "abc", now().minusSeconds(100), now(), now().minusHours(4), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
            new ConversationThread("6", "cba", now().minusHours(10), now(), now().minusHours(5), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
          ),
          180
        );

        List<ConversationThread> conversationThreads = pb.getConversationThreads();

        assertTrue(conversationThreads.size() == 5);

        List<String> containsIds = newArrayList("1", "2", "3", "4", "5", "6");

        conversationThreads.forEach(x -> assertTrue(containsIds.contains(x.getAdId())));
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

    public static class ConversationThread extends AbstractConversationThread {
        public ConversationThread(
          String adId,
          String conversationId,
          DateTime createdAt,
          DateTime modifiedAt,
          DateTime receivedAt,
          boolean containsUnreadMessages,
          Optional<String> previewLastMessage,
          Optional<String> buyerName,
          Optional<String> sellerName,
          Optional<String> buyerId,
          Optional<String> messageDirection) {
            super(
              adId,
              conversationId,
              createdAt,
              modifiedAt,
              receivedAt,
              containsUnreadMessages,
              previewLastMessage,
              buyerName,
              sellerName,
              buyerId,
              messageDirection
            );
        }

        @Override
        public ConversationThread sameButUnread(String message) {
            Optional<String> actualMessage = message == null ? previewLastMessage : Optional.of(message);
            return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), DateTime.now(), true, actualMessage, buyerName, sellerName, buyerId, messageDirection);
        }
    }

    public static AbstractConversationThread createConversationThread(DateTime createdAt, DateTime modifiedAt, String conversationId) {
        DateTime receivedDate = modifiedAt;

        return new PostBoxTest.ConversationThread("123", conversationId, createdAt, modifiedAt, receivedDate, false, Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty());
    }
}
