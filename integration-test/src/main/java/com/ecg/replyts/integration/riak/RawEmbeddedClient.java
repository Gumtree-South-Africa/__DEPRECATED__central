package com.ecg.replyts.integration.riak;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.bucket.BucketProperties;
import com.basho.riak.client.bucket.DefaultBucketProperties;
import com.basho.riak.client.bucket.TunableCAPProps;
import com.basho.riak.client.bucket.VClockPruneProps;
import com.basho.riak.client.cap.Quorum;
import com.basho.riak.client.query.MapReduceResult;
import com.basho.riak.client.query.NodeStats;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.WalkResult;
import com.basho.riak.client.query.indexes.BinIndex;
import com.basho.riak.client.query.indexes.IntIndex;
import com.basho.riak.client.raw.*;
import com.basho.riak.client.raw.query.IndexSpec;
import com.basho.riak.client.raw.query.LinkWalkSpec;
import com.basho.riak.client.raw.query.MapReduceSpec;
import com.basho.riak.client.raw.query.indexes.BinRangeQuery;
import com.basho.riak.client.raw.query.indexes.IndexQuery;
import com.basho.riak.client.raw.query.indexes.IntRangeQuery;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings("all") // NOSONAR
public class RawEmbeddedClient implements RawClient { // NOSONAR

    private ClientDelegate clientDelegate = new ClientDelegate();

    @Override
    public RiakResponse head(String bucket, String key, FetchMeta fetchMeta) throws IOException {
        RiakResponse fetch = fetch(bucket, key);
        return new RiakResponse(fetch.getVclockBytes(), new IRiakObject[]{});
    }

    @Override
    public RiakResponse fetch(String bucket, String key) throws IOException {
        return clientDelegate.fetch(bucket, key);
    }

    @Override
    public RiakResponse fetch(String bucket, String key, int readQuorum) throws IOException {

        return fetch(bucket, key);
    }

    @Override
    public RiakResponse fetch(String bucket, String key, FetchMeta fetchMeta) throws IOException {
        return fetch(bucket, key);
    }

    @Override
    public RiakResponse store(IRiakObject object, StoreMeta storeMeta) throws IOException {
        return clientDelegate.store(object);
    }

    @Override
    public void store(IRiakObject object) throws IOException {
        store(object, null);
    }

    @Override
    public void delete(String bucket, String key) throws IOException {
        clientDelegate.delete(bucket, key);
    }

    @Override
    public void delete(String bucket, String key, int deleteQuorum) throws IOException {
        delete(bucket, key);
    }

    @Override
    public void delete(String bucket, String key, DeleteMeta deleteMeta) throws IOException {
        delete(bucket, key);
    }

    @Override
    public Set<String> listBuckets() {

        return clientDelegate.buckets();
    }

    @Override
    public StreamingOperation<String> listBucketsStreaming() throws IOException {
        throw new UnsupportedOperationException("listing buckets not supported in embedded client");
    }

    @Override
    public BucketProperties fetchBucket(String bucketName) {
        Quorum one = new Quorum(1);
        return new DefaultBucketProperties(true, true, 1, "eleveldb", new VClockPruneProps(1, 2, 3l, 4l), null, null, new TunableCAPProps(one, one, one, one, one, one, true, true), null, null, false);
    }

    @Override
    public void updateBucket(String name, BucketProperties bucketProperties) {
    }

    @Override
    public void resetBucketProperties(String s) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StreamingOperation<String> listKeys(String bucketName) {
        return new ListStreamingOperation<String>(Lists.newArrayList(clientDelegate.keys(bucketName)));
    }

    @Override
    public WalkResult linkWalk(LinkWalkSpec linkWalkSpec) {
        throw new UnsupportedOperationException("Link Walking not supported in Embedded Client");
    }

    @Override
    public MapReduceResult mapReduce(MapReduceSpec spec) {
        throw new UnsupportedOperationException("Map Reduce not supported in Embedded Client");
    }

    @Override
    public byte[] generateAndSetClientId() throws IOException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setClientId(byte[] clientId) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public byte[] getClientId() throws IOException {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void ping() {
    }

    @Override
    public List<String> fetchIndex(IndexQuery indexQuery) throws IOException {
        return clientDelegate.fetchIndex(indexQuery);
    }

    @Override
    public StreamingOperation<IndexEntry> fetchIndex(IndexSpec indexSpec) throws IOException {
        IndexQuery qry = null;


        if (indexSpec.isBinIndex()) {
            qry = new BinRangeQuery(BinIndex.named(indexSpec.getIndexName()), indexSpec.getBucketName(), indexSpec.getRangeStart(), indexSpec.getRangeEnd());
        } else {
            qry = new IntRangeQuery(IntIndex.named(indexSpec.getIndexName()), indexSpec.getBucketName(), Long.valueOf(indexSpec.getRangeStart()), Long.valueOf(indexSpec.getRangeEnd()));
        }

        Preconditions.checkNotNull(qry, "this query type is not supported in embedded client");
        return new ListStreamingOperation<IndexEntry>(Lists.newArrayList(Lists.transform(clientDelegate.fetchIndex(qry), new Function<String, IndexEntry>() {
            @Nullable
            @Override
            public IndexEntry apply(@Nullable String s) {
                return new IndexEntry(s);
            }
        })));
    }

    @Override
    public Long incrementCounter(String s, String s2, long l, StoreMeta storeMeta) throws IOException {
        throw new UnsupportedOperationException("counters not supported in embedded client");
    }

    @Override
    public Long fetchCounter(String s, String s2, FetchMeta fetchMeta) throws IOException {
        throw new UnsupportedOperationException("counters not supported in embedded client");

    }

    @Override
    public Transport getTransport() {
        return Transport.HTTP;
    }

    @Override
    public void shutdown() {
        clientDelegate.deleteAll();
    }

    @Override
    public NodeStats stats() throws IOException {
        throw new UnsupportedOperationException("no node stats in embedded mode: no node is there ;) ");
    }

    class ListStreamingOperation<T> implements StreamingOperation<T> {
        private final List<T> content;
        private final Iterator<T> runningIterator;

        ListStreamingOperation(List<T> content) {

            this.content = ImmutableList.copyOf(content);
            this.runningIterator = this.content.iterator();
        }

        @Override
        public List<T> getAll() {
            return content;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean hasContinuation() {
            return false;
        }

        @Override
        public String getContinuation() {
            return null;
        }

        @Override
        public Iterator<T> iterator() {
            return content.iterator();
        }

        @Override
        public boolean hasNext() {
            return runningIterator.hasNext();
        }

        @Override
        public T next() {
            return runningIterator.next();
        }

        @Override
        public void remove() {
            runningIterator.remove();
        }
    }
}
