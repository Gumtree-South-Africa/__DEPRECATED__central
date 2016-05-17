package com.ecg.messagecenter.persistence.riak;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DefaultRiakPostBoxRepositoryIntegrationTest {

    private static final String FOO_BAR_POST_BOX_ID = "foo@bar.com";

    private final IRiakClient riakClient = new EmbeddedRiakClient();

    private RiakPostBoxRepository postBoxRepository;

    @Before
    public void setup() {
        this.postBoxRepository = new DefaultRiakPostBoxRepository(riakClient, null);
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void getByIdAlwaysReturnsNotNull() {
        PostBox nonExistent = postBoxRepository.getPostBox("nonexistent");
        assertNotNull(nonExistent);
        assertNotNull(nonExistent.getNewRepliesCounter());
        assertEquals(0, nonExistent.getNewRepliesCounter());
        assertNotNull(nonExistent.getConversationThreads());
        assertTrue(nonExistent.getConversationThreads().isEmpty());
    }

    @Test
    public void unreadCounts() {
        PostBox postBox = createPostBox(3, FOO_BAR_POST_BOX_ID);
        postBoxRepository.write(postBox);

        PostBox reloadedPostBox = postBoxRepository.getPostBox(FOO_BAR_POST_BOX_ID);

        assertNotNull(reloadedPostBox);
        assertEquals(FOO_BAR_POST_BOX_ID, reloadedPostBox.getUserId());

        assertEquals(3, reloadedPostBox.getConversationThreads().size());
        assertEquals(2, reloadedPostBox.getNumUnreadConversations());
        assertEquals(3, reloadedPostBox.getNewRepliesCounter());

        PostBoxUnreadCounts unreadCounts = postBoxRepository.getUnreadCounts(FOO_BAR_POST_BOX_ID);
        assertEquals(2, unreadCounts.getNumUnreadConversations());
        assertEquals(3, unreadCounts.getNumUnreadMessages());

        assertEquals(0, reloadedPostBox.getConversationThreads().get(0).getNumUnreadMessages());
        assertEquals(1, reloadedPostBox.getConversationThreads().get(1).getNumUnreadMessages());
        assertEquals(2, reloadedPostBox.getConversationThreads().get(2).getNumUnreadMessages());
    }

    @Test
    public void cleansUpPostBox() {
        PostBox box = createPostBox(2, FOO_BAR_POST_BOX_ID);
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now());

        PostBox reloaded = postBoxRepository.getPostBox(FOO_BAR_POST_BOX_ID);
        assertNotNull(reloaded);
    }

    @Test
    public void keepsPostBoxEntriesThatAreTooNew() throws RiakRetryFailedException {
        PostBox box = createPostBox(2, FOO_BAR_POST_BOX_ID);
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now().minusSeconds(10));

        PostBox reloaded = postBoxRepository.getPostBox(FOO_BAR_POST_BOX_ID);

        assertNotNull(reloaded);
    }

    private PostBox createPostBox(int numConversations, String postBoxId) {
        List<ConversationThread> conversations = new ArrayList<>();
        for (int i = 0; i < numConversations; i++) {
            DateTime receivedAt = now().minusMinutes(i + 1);

            ConversationThread ct = new ConversationThread(
                    "adId", "conversationId" + i,
                    receivedAt, receivedAt, receivedAt, i,
                    Optional.of("Message"), Optional.of("buyer name"), Optional.of("seller name"),
                    Optional.of("buyerId"), Optional.of("sellerId"), Optional.<Long>absent(),
                    Optional.<Long>absent(), Optional.<Long>absent(), Optional.of(receivedAt));
            conversations.add(ct);
        }
        return new PostBox(postBoxId, conversations);
    }

}