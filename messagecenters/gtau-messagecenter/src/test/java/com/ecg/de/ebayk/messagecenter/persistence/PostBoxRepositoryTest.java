package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Collections;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

public class PostBoxRepositoryTest {

    private final IRiakClient riakClient = new EmbeddedRiakClient();
    private PostBoxRepository postBoxRepository = new PostBoxRepository(riakClient, 100);


    @Test
    public void persistsPostbox() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.<Long>absent(), Collections.<ConversationThread>emptyList());
        postBoxRepository.write(box);

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNotNull(postbox);
        assertEquals("foo@bar.com", postbox.getKey());
        assertEquals("application/x-gzip", postbox.getContentType());
    }

    @Test
    public void cleansUpPostbox() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.<Long>absent(), Collections.<ConversationThread>emptyList());
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now());

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNull(postbox);
    }

    @Test
    public void keepsPostboxEntriesThatAreTooNew() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.<Long>absent(), Collections.<ConversationThread>emptyList());
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now().minusSeconds(1));

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNotNull(postbox);

    }
}
