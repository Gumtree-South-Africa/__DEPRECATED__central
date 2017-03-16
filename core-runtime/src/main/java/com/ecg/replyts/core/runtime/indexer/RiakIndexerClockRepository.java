package com.ecg.replyts.core.runtime.indexer;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import org.joda.time.DateTime;

public class RiakIndexerClockRepository implements IndexerClockRepository {

    private static final String LAST_RUN_KEY = "lastRun";
    private static final String INDEXER_CLOCK_BUCKET = "indexerClockBucket";

    private final Bucket indexerClockBucket;

    public RiakIndexerClockRepository(IRiakClient riakClient, String bucketNamePrefix) throws RiakRetryFailedException {
        indexerClockBucket = riakClient.createBucket(bucketNamePrefix + INDEXER_CLOCK_BUCKET).allowSiblings(false).execute();
    }

    @Override
    public void set(DateTime lastRun) {
        try {
            indexerClockBucket.store(LAST_RUN_KEY, lastRun.getMillis()).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {
        try {
            indexerClockBucket.delete(LAST_RUN_KEY).execute();
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime get() {
        try {
            IRiakObject result = indexerClockBucket.fetch(LAST_RUN_KEY).execute();
            if (result == null) {
                return null;
            }
            String lastRunContent = result.getValueAsString();
            return new DateTime(Long.valueOf(lastRunContent));
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }


}
