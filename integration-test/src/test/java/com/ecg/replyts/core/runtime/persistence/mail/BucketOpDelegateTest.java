package com.ecg.replyts.core.runtime.persistence.mail;


import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import org.junit.Before;
import org.junit.Test;

import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BucketOpDelegateTest {

    private static final String MAIL_BUCKET_NAME = "mail";
    private EmbeddedRiakClient client;
    private BucketOpDelegate delegate;

    @Before
    public void setUp() throws Exception {
        client = new EmbeddedRiakClient();
        delegate = new BucketOpDelegate(MAIL_BUCKET_NAME, client);
    }

    @Test
    public void deletesMails() throws Exception {
        delegate.storeMailAsSingleObject(now().minusDays(10), "delete1", "payload".getBytes());
        delegate.storeMailAsSingleObject(now().minusDays(10), "delete2", "payload".getBytes());
        delegate.storeMailAsSingleObject(now(), "stayId", "payload".getBytes());

        delegate.deleteMailsByOlderThan(now().minusDays(4), 10, 2);

        assertNull(delegate.fetchKey("delete1"));
        assertNull(delegate.fetchKey("delete2"));
        assertNotNull(delegate.fetchKey("stayId"));
    }
}
