package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.BasicVClock;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.DateTime.now;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:16
 *
 * @author maldana@ebay.de
 */
public class PostBoxConverterTest {

    public static final long START_DATE_MILLIS = 1391368984813l;
    public static final int MAX_CONVERSATION_AGE_DAYS =
            Days.daysBetween(new DateTime(START_DATE_MILLIS), DateTime.now()).getDays() + 1;
    private DateTime created = DateTime.now();

    private PostBoxConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new PostBoxConverter(PostBoxRepository.POST_BOX, MAX_CONVERSATION_AGE_DAYS);
    }

    @Test
    public void constructDeconstructPostboxPlainString() {
        PostBox postBox = createPostBox(MAX_CONVERSATION_AGE_DAYS);

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        // overwriting value with string json old style
        riakObject.setValue("{\"version\":1,\"threads\":[{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}]}\n");

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(2, postBox.getConversationThreads().size());
        assertEquals(postBox.getConversationThreads().get(0).getConversationId(),
                converted.getConversationThreads().get(0).getConversationId());
        assertEquals(postBox.getEmail(), converted.getEmail());
        // WARNING: normal assert equals does not work any more due to the timestamp that is changed when creating a new post box
    }

    @Test
    public void constructOldConversationsPostboxPlainString() {
        PostBox postBox = createPostBox(1);

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(0, postBox.getConversationThreads().size());
        assertEquals(0, converted.getConversationThreads().size());
        assertEquals(postBox.getEmail(), converted.getEmail());
        // WARNING: normal assert equals does not work any more due to the timestamp that is changed when creating a new post box
    }

    @Test
    public void constructDeconstructPostboxGzipped() {
        PostBox postBox = createPostBox(MAX_CONVERSATION_AGE_DAYS);

        PostBoxConverter converter = new PostBoxConverter(PostBoxRepository.POST_BOX, MAX_CONVERSATION_AGE_DAYS);
        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        riakObject.setValue(GzipAwareContentFilter.compress("{\"version\":1,\"threads\":[{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}]}\n"));
        riakObject.setContentType("application/x-gzip");

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(2, converted.getConversationThreads().size());
        assertEquals(postBox.getConversationThreads().get(0).getConversationId(),
                converted.getConversationThreads().get(0).getConversationId());
        assertEquals(postBox.getEmail(), converted.getEmail());
    }

    @Test
    public void receivedAtIsNullSafeDeconstruct() {
        PostBox postBox = new PostBox.PostBoxBuilder().withEmail("bla@blah.com").withNewRepliesCounter(1l).withConversationThreads(Lists.newArrayList(
                new ConversationThread("321", "cba", now(), now(), null, false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent()))).build();

        PostBoxConverter converter = new PostBoxConverter(PostBoxRepository.POST_BOX, MAX_CONVERSATION_AGE_DAYS);
        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
    }

    private PostBox createPostBox(int maxAgeDays) {
        DateTime dt = new DateTime();
        DateTime createdAt1 = dt.withMillis(1391368984813L);
        DateTime createdAt2 = dt.withMillis(1391368984879L);
        DateTime modifiedAt1 = dt.withMillis(1391368984875L);
        DateTime modifiedAt2 = dt.withMillis(1391368984879L);
        DateTime receivedAt1 = dt.withMillis(1391365384875L);
        DateTime receivedAt2 = dt.withMillis(1391332984879L);

        return new PostBox.PostBoxBuilder().withEmail("bla@blah.com").withNewRepliesCounter(1L).withMaxConversationAgeDays(maxAgeDays).withConversationThreads(Lists.newArrayList(
                new ConversationThread("123", "abc", createdAt1, modifiedAt1, receivedAt1, true, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("blah@blah.com"), Optional.<String>absent()),
                new ConversationThread("321", "cba", createdAt2, modifiedAt2, receivedAt2, false, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.of("seller@example.com"), Optional.<String>absent())
        )).build();
    }

}
