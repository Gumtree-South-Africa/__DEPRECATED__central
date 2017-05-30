package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReadOnlyRiakMailRepository extends DiffingRiakMailRepository {


    private static final Logger LOG = LoggerFactory.getLogger(ReadOnlyRiakMailRepository.class);

    public ReadOnlyRiakMailRepository(String bucketPrefix, IRiakClient riakClient) throws RiakRetryFailedException {
        this(new MailBucket(bucketPrefix, riakClient));
    }

    ReadOnlyRiakMailRepository(MailBucket mailBucket) throws RiakRetryFailedException {
       super(mailBucket);
    }

    @Override
    protected void doPersist(String messageId, byte[] compress) {
        LOG.debug("called doPersist");
    }

    @Override
    public void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
        LOG.debug("called deleteMailsByOlderThan");
    }

    @Override
    public void deleteMail(String messageId) {
        LOG.debug("called deleteMail");
    }

}
