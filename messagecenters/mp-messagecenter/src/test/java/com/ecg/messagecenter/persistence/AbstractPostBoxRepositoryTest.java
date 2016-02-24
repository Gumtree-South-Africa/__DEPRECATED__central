package com.ecg.messagecenter.persistence;

import com.basho.riak.client.RiakRetryFailedException;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

public abstract class AbstractPostBoxRepositoryTest<T extends PostBoxRepository> {

    private T postBoxRepository;

    protected abstract T createPostBoxRepository();

    @Before
    public void setup() {
        this.postBoxRepository = createPostBoxRepository();
    }

    @Test
    public void persistsPostbox() {
        PostBox box = createPostBox(1);
        postBoxRepository.write(box);

        PostBox reloaded = postBoxRepository.byId("foo@bar.com");

        assertNotNull(reloaded);
        assertEquals("foo@bar.com", reloaded.getUserId());
        assertEquals(1, reloaded.getConversationThreads().size());
    }

    private PostBox createPostBox(int numConversations) {
        List<ConversationThread> conversations = new ArrayList<>();
        for (int i = numConversations; i > 0; i--) {
            DateTime receivedAt = now().minusSeconds(5 * i);

            ConversationThread ct = new ConversationThread(true, "adId", "conversationId" + i,
                    receivedAt, receivedAt, receivedAt, 1,
                    Optional.of("Message"), Optional.of("buyer name"), Optional.of("seller name"),
                    Optional.of("buyerId"), Optional.of("sellerId"), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(receivedAt));
            conversations.add(ct);
        }
        return new PostBox("foo@bar.com", conversations);
    }

    @Test
    public void getByIdAlwaysReturnsNotNull() {
        PostBox nonExistent = postBoxRepository.byId("nonexistent");
        assertNotNull(nonExistent);
        assertNotNull(nonExistent.getNewRepliesCounter());
        assertEquals(0, nonExistent.getNewRepliesCounter().longValue());
        assertNotNull(nonExistent.getConversationThreads());
        assertTrue(nonExistent.getConversationThreads().isEmpty());
    }

    @Test
    public void cleansUpPostbox() {
        PostBox box = createPostBox(2);
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now());

        PostBox reloaded = postBoxRepository.byId("foo@bar.com");
        assertNotNull(reloaded);
    }

    @Test
    public void keepsPostboxEntriesThatAreTooNew() throws RiakRetryFailedException {
        PostBox box = createPostBox(2);
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now().minusSeconds(10));

        PostBox reloaded = postBoxRepository.byId("foo@bar.com");

        assertNotNull(reloaded);
    }
}