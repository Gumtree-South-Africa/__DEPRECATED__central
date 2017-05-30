package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.core.api.persistence.MailRepository;
import org.joda.time.DateTime;

import java.util.Optional;

public class DiffingRiakMailRepository extends AbstractMailRepository implements MailRepository {
    private final MailBucket mailBucket;

    public DiffingRiakMailRepository(String bucketPrefix, IRiakClient riakClient) throws RiakRetryFailedException {
        this(new MailBucket(bucketPrefix, riakClient));
    }

    DiffingRiakMailRepository(MailBucket mailBucket) throws RiakRetryFailedException {
        this.mailBucket = mailBucket;
    }

    @Override
    protected void doPersist(String messageId, byte[] compress) {
        mailBucket.persistMail(DateTime.now(), messageId, compress);
    }

    @Override
    protected Optional<byte[]> doLoad(String messageId) {
        return mailBucket.load(messageId);
    }

    @Override
    public void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
        mailBucket.deleteMailsByOlderThan(time, maxResults, numCleanUpThreads);
    }

    @Override
    public void deleteMail(String messageId) {
        mailBucket.delete(messageId);
    }
}