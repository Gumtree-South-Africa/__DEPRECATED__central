package com.ecg.replyts.integration.riak;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.raw.RiakResponse;
import com.basho.riak.client.raw.query.indexes.*;
import com.ecg.replyts.integration.riak.persistence.Bucket;
import com.ecg.replyts.integration.riak.persistence.Storage;
import com.ecg.replyts.integration.riak.persistence.Value;
import com.google.common.collect.Range;

import java.util.List;
import java.util.Set;

class ClientDelegate {
    private final Storage storage = new Storage();


    public RiakResponse store(IRiakObject object) {
        Value val = new Value(object.getValue(), object.getContentType(), object.allBinIndexes(), object.allIntIndexesV2());
        storage.getBucket(object.getBucket()).put(object.getKey(), val);
        return new RiakResponse(val.getVClock(), new IRiakObject[]{object});
    }

    public void delete(String bucket, String key) {
        storage.getBucket(bucket).delete(key);
    }

    public Set<String> buckets() {
        return storage.buckets();
    }

    public Iterable<String> keys(String bucketName) {
        return storage.getBucket(bucketName).keys();
    }

    public RiakResponse fetch(String bucket, String key) {
        Value val = storage.getBucket(bucket).get(key);
        if (val == null) {
            return RiakResponse.empty();
        }


        return new RiakResponse(val.getVClock(), new IRiakObject[]{
                RiakObjectBuilder
                        .newBuilder(bucket, key)
                        .withVClock(val.getVClock())
                        .withValue(val.getValue())
                        .withContentType(val.getProperties().get("Content-Type"))
                        .build()
        });
    }


    public List<String> fetchIndex(IndexQuery indexQuery) {

        Bucket bucket = storage.getBucket(indexQuery.getBucket());

        if (indexQuery instanceof BinRangeQuery) {

            return bucket.fetch2iBin(indexQuery.getIndex(), Range.closed(((BinRangeQuery) indexQuery).from(), ((BinRangeQuery) indexQuery).to()));
        } else if (indexQuery instanceof BinValueQuery) {
            return bucket.fetch2iBin(indexQuery.getIndex(), Range.closed(((BinValueQuery) indexQuery).getValue(), ((BinValueQuery) indexQuery).getValue()));
        } else if (indexQuery instanceof IntRangeQuery) {
            return bucket.fetch2iLong(indexQuery.getIndex(), Range.closed(((IntRangeQuery) indexQuery).from(), ((IntRangeQuery) indexQuery).to()));
        } else if (indexQuery instanceof IntValueQuery) {
            return bucket.fetch2iLong(indexQuery.getIndex(), Range.closed(((IntValueQuery) indexQuery).getValue(), ((IntValueQuery) indexQuery).getValue()));
        }

        throw new UnsupportedOperationException("can't handle this index query: " + indexQuery);
    }

    public void deleteAll() {
        storage.deleteAll();
    }
}
