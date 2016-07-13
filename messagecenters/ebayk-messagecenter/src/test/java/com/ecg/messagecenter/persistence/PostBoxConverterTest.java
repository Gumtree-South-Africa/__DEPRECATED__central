package com.ecg.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.BasicVClock;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

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

    private DateTime created = DateTime.now();

    private PostBoxConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new PostBoxConverter();
    }

    @Test
    public void constructDeconstructPostboxPlainString() {
        PostBox postBox = createPostBox();

        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        // overwriting value with string json old style
        riakObject.setValue("{\"version\":1,\"threads\":[{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}]}\n");

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
        // WARNING: normal assert equals does not work any more due to the timestamp that is changed when creating a new post box
    }

    @Test
    public void constructDeconstructPostboxGzipped(){
        PostBox postBox = createPostBox();

        PostBoxConverter converter = new PostBoxConverter();
        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));
        riakObject.setValue(GzipAwareContentFilter.compress("{\"version\":1,\"threads\":[{\"adId\":\"123\",\"createdAt\":1391368984813,\"modifiedAt\":1391368984875,\"conversationId\":\"abc\",\"containsUnreadMessages\":true,\"receivedAt\":1391365384875},{\"adId\":\"321\",\"createdAt\":1391368984879,\"modifiedAt\":1391368984879,\"conversationId\":\"cba\",\"containsUnreadMessages\":false,\"receivedAt\":1391332984879}]}\n"));
        riakObject.setContentType("application/x-gzip");

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
    }

    @Test
    public void receivedAtIsNullSafeDeconstruct(){
        PostBox postBox = new PostBox("bla@blah.com", Optional.of(1l), Lists.newArrayList(
                new ConversationThread("321", "cba", now(), now(), null, false, Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<Long>empty(), Optional.<Long>empty(), Optional.<Long>empty())));

        PostBoxConverter converter = new PostBoxConverter();
        IRiakObject riakObject = converter.fromDomain(postBox, new BasicVClock(new byte[]{}));

        PostBox converted = converter.toDomain(riakObject);

        assertEquals(postBox.getConversationThreads(), converted.getConversationThreads());
        assertEquals(postBox.getEmail(), converted.getEmail());
    }

    private PostBox createPostBox() {
        DateTime dt = new DateTime();
        DateTime createdAt1 = dt.withMillis(1391368984813L);
        DateTime createdAt2 = dt.withMillis(1391368984879L);
        DateTime modifiedAt1 = dt.withMillis(1391368984875L);
        DateTime modifiedAt2 = dt.withMillis(1391368984879L);
        DateTime receivedAt1 = dt.withMillis(1391365384875L);
        DateTime receivedAt2 = dt.withMillis(1391332984879L);

        return new PostBox("bla@blah.com", Optional.of(1L), Lists.newArrayList(
                new ConversationThread("123", "abc", createdAt1, modifiedAt1, receivedAt1, true, Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<Long>empty(), Optional.<Long>empty(), Optional.<Long>empty()),
                new ConversationThread("321", "cba", createdAt2, modifiedAt2, receivedAt2, false, Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<String>empty(), Optional.<Long>empty(), Optional.<Long>empty(), Optional.<Long>empty())
        ));
    }






}
