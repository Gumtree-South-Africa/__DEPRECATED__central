package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.*;

public class PostBoxRepositoryTest {

    private final IRiakClient riakClient = new EmbeddedRiakClient();
    private PostBoxRepository postBoxRepository = new PostBoxRepository(riakClient);


    @Test
    public void persistsPostbox() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<ConversationThread>emptyList());
        postBoxRepository.write(box);

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNotNull(postbox);
        assertEquals("foo@bar.com", postbox.getKey());
        assertEquals("application/x-gzip", postbox.getContentType());
    }

    @Test
    public void cleansUpPostbox() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<ConversationThread>emptyList());
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now(UTC));

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNull(postbox);
    }

    @Test
    public void keepsPostboxEntriesThatAreTooNew() throws RiakRetryFailedException {
        PostBox box = new PostBox("foo@bar.com", Optional.empty(), Collections.<ConversationThread>emptyList());
        postBoxRepository.write(box);

        postBoxRepository.cleanupLongTimeUntouchedPostBoxes(now(UTC).minusSeconds(1));

        IRiakObject postbox = riakClient.fetchBucket("postbox").execute().fetch("foo@bar.com").execute();

        assertNotNull(postbox);

    }
}
