package com.ecg.de.ebayk.messagecenter.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

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
    public static final int DEFAULT_MAX_AGE_DAYS = 30;

    private PostBoxConflictResolver conflictResolver(int days) {
        return new PostBoxConflictResolver(days);
    }

    @Test
    public void disjunctPostBoxes() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);

        PostBox postBox1 = buildPostBox(twoHoursAgo, 1, "a:1", DEFAULT_MAX_AGE_DAYS);
        PostBox postBox2 = buildPostBox(threeHoursAgo, 1, "b:2", DEFAULT_MAX_AGE_DAYS);
        PostBox resolvedPostBox = conflictResolver(DEFAULT_MAX_AGE_DAYS).resolve(Lists.newArrayList(postBox1, postBox2));

        List<ConversationThread> conversations = Lists.newArrayList(createConvThread(twoHoursAgo, "a:1", CREATED_AT),
                createConvThread(threeHoursAgo, "b:2", CREATED_AT));
        PostBox expected = buildPostBox(1, DEFAULT_MAX_AGE_DAYS, conversations);
        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void usesLatestThreadVersionWhenInFirstThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);

        PostBox postBox1 = buildPostBox(twoHoursAgo, 1, "a:1", DEFAULT_MAX_AGE_DAYS);
        PostBox postBox2 = buildPostBox(threeHoursAgo, 1, "a:1", DEFAULT_MAX_AGE_DAYS);
        PostBox resolvedPostBox = conflictResolver(DEFAULT_MAX_AGE_DAYS).resolve(Lists.newArrayList(postBox1, postBox2));

        List<ConversationThread> conversations = Lists.newArrayList(createConvThread(twoHoursAgo, "a:1", CREATED_AT));
        PostBox expected = buildPostBox(1, DEFAULT_MAX_AGE_DAYS, conversations);
        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void usesLatestThreadVersionWhenInSecondThread() {
        DateTime twoHoursAgo = now().minusHours(2);
        DateTime threeHoursAgo = now().minusHours(5);

        PostBox postBox1 = buildPostBox(twoHoursAgo, 3, "a:1", DEFAULT_MAX_AGE_DAYS);
        PostBox postBox2 = buildPostBox(threeHoursAgo, 2, "a:1", DEFAULT_MAX_AGE_DAYS);
        PostBox resolvedPostBox = conflictResolver(DEFAULT_MAX_AGE_DAYS).resolve(Lists.newArrayList(postBox1, postBox2));

        List<ConversationThread> conversations = Lists.newArrayList(createConvThread(twoHoursAgo, "a:1", CREATED_AT));
        PostBox expected = buildPostBox(3, DEFAULT_MAX_AGE_DAYS, conversations);

        assertEquals(expected, resolvedPostBox);
    }

    @Test
    public void returnNewConversationsWhenOldConversationsExist() {
        DateTime yesterday = now().minusDays(1).minusHours(2);
        DateTime now = now();

        List<ConversationThread> oldConversations = Lists.newArrayList(createConvThread(yesterday, "a:1", yesterday));
        PostBox postBox1 = buildPostBox(1, 1, oldConversations);

        List<ConversationThread> newConversations = Lists.newArrayList(createConvThread(now, "a:1", now));
        PostBox postBox2 = buildPostBox(1, 1, newConversations);

        PostBox resolvedPostBox = conflictResolver(1).resolve(Lists.newArrayList(postBox1, postBox2));

        PostBox expected = buildPostBox(1, DEFAULT_MAX_AGE_DAYS, newConversations);

        assertEquals(expected, resolvedPostBox);

    }

    private ConversationThread createConvThread(DateTime modifiedAt, String convId, DateTime createdAt) {
        DateTime receivedDate = modifiedAt;
        return new ConversationThread("123", convId, createdAt, modifiedAt, receivedDate, false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent());
    }

    private PostBox buildPostBox(DateTime modifiedAt, long repliesCounter, String convId, int maxAgeDays) {
        return buildPostBox(repliesCounter, maxAgeDays, Lists.newArrayList(createConvThread(modifiedAt, convId, CREATED_AT)));
    }

    private PostBox buildPostBox(long repliesCounter, int maxAgeDays, List<ConversationThread> conversationThreads) {
        return new PostBox.PostBoxBuilder().withEmail("foo@bar.de").
                withNewRepliesCounter(repliesCounter).
                withConversationThreads(conversationThreads).
                withMaxConversationAgeDays(maxAgeDays).build();
    }
}
