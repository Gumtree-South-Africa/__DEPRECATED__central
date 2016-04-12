package com.ecg.replyts.integration.riak;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.bucket.FetchBucket;
import com.basho.riak.client.bucket.WriteBucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Retrier;
import com.basho.riak.client.query.BucketKeyMapReduce;
import com.basho.riak.client.query.BucketMapReduce;
import com.basho.riak.client.query.IndexMapReduce;
import com.basho.riak.client.query.LinkWalk;
import com.basho.riak.client.query.NodeStats;
import com.basho.riak.client.query.SearchMapReduce;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.raw.RawClient;
import com.basho.riak.client.raw.Transport;
import com.basho.riak.client.raw.query.indexes.IndexQuery;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;


public final class EmbeddedRiakClient implements IRiakClient { // NOSONAR

    private volatile RawClient rawClient = new RawEmbeddedClient();
    private final Retrier retrier = DefaultRetrier.attempts(3);


    @Deprecated
    public Set<String> listBuckets() throws RiakException {
        try {
            return rawClient.listBuckets();
        } catch (IOException e) {
            throw new RiakException(e);
        }
    }

    @Override
    public StreamingOperation<String> listBucketsStreaming() throws RiakException {
        try {
            return rawClient.listBucketsStreaming();
        } catch (IOException e) {
            throw new RiakException(e);
        }
    }

    public WriteBucket updateBucket(final Bucket b) {
        return new WriteBucket(rawClient, b, retrier);
    }

    public FetchBucket fetchBucket(String bucketName) {
        return new FetchBucket(rawClient, bucketName, retrier);
    }

    public WriteBucket createBucket(String bucketName) {
        return new WriteBucket(rawClient, bucketName, retrier);
    }

    @Override
    public void resetBucket(String bucketName) throws RiakException {
        try {
            rawClient.resetBucketProperties(bucketName);
        } catch (IOException e) {
            throw new RiakException(e);
        }
    }

    public IRiakClient setClientId(final byte[] clientId) throws RiakException {
        if (clientId == null || clientId.length != 4) {
            throw new IllegalArgumentException("Client Id must be 4 bytes long");
        }
        final byte[] cloned = clientId.clone();
        retrier.attempt(new Callable<Void>() {
            public Void call() throws Exception {
                rawClient.setClientId(cloned);
                return null;
            }
        });

        return this;
    }


    public byte[] generateAndSetClientId() throws RiakException {
        final byte[] clientId = retrier.attempt(new Callable<byte[]>() {
            public byte[] call() throws Exception {
                return rawClient.generateAndSetClientId();
            }
        });

        return clientId;
    }

    public byte[] getClientId() throws RiakException {
        final byte[] clientId = retrier.attempt(new Callable<byte[]>() {
            public byte[] call() throws Exception {
                return rawClient.getClientId();
            }
        });

        return clientId;
    }


    public BucketKeyMapReduce mapReduce() {
        return new BucketKeyMapReduce(rawClient);
    }


    public BucketMapReduce mapReduce(String bucket) {
        return new BucketMapReduce(rawClient, bucket);
    }


    public SearchMapReduce mapReduce(String bucket, String query) {
        return new SearchMapReduce(rawClient, bucket, query);
    }


    public IndexMapReduce mapReduce(IndexQuery query) {
        return new IndexMapReduce(rawClient, query);
    }


    public LinkWalk walk(IRiakObject startObject) {
        return new LinkWalk(rawClient, startObject);
    }


    public void ping() throws RiakException {
        try {
            rawClient.ping();
        } catch (IOException e) {
            throw new RiakException(e);
        }
    }

    public Transport getTransport() {
        return rawClient.getTransport();
    }

    public void shutdown() {
        rawClient.shutdown();
    }

    public Iterable<NodeStats> stats() throws RiakException {
        return Lists.newArrayList();
    }
}