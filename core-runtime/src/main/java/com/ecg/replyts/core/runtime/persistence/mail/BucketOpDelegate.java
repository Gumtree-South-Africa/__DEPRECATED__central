package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


import static com.basho.riak.client.builders.RiakObjectBuilder.newBuilder;
import static com.ecg.replyts.core.runtime.persistence.FetchIndexHelper.fetchResult;
import static com.ecg.replyts.core.runtime.persistence.FetchIndexHelper.logInterval;
import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

class BucketOpDelegate {

    private static final Timer DELETE_MAIL = TimingReports.newTimer("cleanupMail");

    private static final String CREATED_INDEX = "created";
    private static final Joiner MESSAGE_ID_CHUNK_JOINER = Joiner.on("@Part");

    private final Bucket mailBucket;

    private static final Logger LOG = LoggerFactory.getLogger(BucketOpDelegate.class);
    private final String bucketName;

    void storeMailAsSingleObject(DateTime messageCreationDate, String messageId, byte[] contents) {
        try {
            mailBucket.store(
                    newBuilder(bucketName, messageId)
                            .withValue(contents)
                            .withContentType(MailBucket.REPLYTS_MAIL_COMPLETE)
                            .addIndex(CREATED_INDEX, timestampInMinutes(messageCreationDate))
                            .build()
            ).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("Could not store mail: ", e);
        }
    }

    String storeOneChunk(DateTime messageCreationDate, String messageId, Chunks.Chunk chunk) {
        String key = MESSAGE_ID_CHUNK_JOINER.join(messageId, chunk.getIndex());
        try {
            mailBucket.store(
                    newBuilder(bucketName, key)
                            .withValue(chunk.getContents())
                            .withContentType(MailBucket.REPLYTS_MAIL_CHUNK)
                            .addIndex(CREATED_INDEX, timestampInMinutes(messageCreationDate))
                            .build()
            ).execute();

            return key;
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(String.format("Failed to store mail '%s': could not write chunk: '%s'", messageId, key), e);
        }
    }

    void storeManifest(DateTime messageCreationDate, String messageId, Manifest manifest) {
        try {
            mailBucket.store(
                    newBuilder(bucketName, messageId)
                            .withValue(manifest.generate())
                            .withContentType(MailBucket.REPLYTS_MAIL_MANIFEST)
                            .addIndex(CREATED_INDEX, timestampInMinutes(messageCreationDate))
                            .build()
            ).execute();

        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(String.format("Failed to store mail '%s': could not write manifest", messageId), e);
        }
    }


    IRiakObject fetchKey(String messageId) {
        try {

            return mailBucket
                    .fetch(messageId)
                    .r(1)
                    .notFoundOK(false)
                    .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load mail '" + messageId + "'", e);
        }
    }

    byte[] fetchChunk(String chunk) {
        try {

            IRiakObject result = mailBucket
                    .fetch(chunk)
                    .r(1)
                    .notFoundOK(false)
                    .execute();
            if (result == null) {
                throw new RuntimeException(String.format("can not load mail because chunk '%s is missing", chunk));
            }
            return result.getValue();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load mail chunk'" + chunk + "'", e);
        }

    }

    BucketOpDelegate(String bucketName, IRiakClient riakClient) {
        this.bucketName = bucketName;
        try {
            this.mailBucket = riakClient.fetchBucket(bucketName).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    void delete(String messageId) {
        try (Timer.Context ignored = DELETE_MAIL.time()) {
            mailBucket.delete(messageId).w(1).r(1).rw(1).dw(0).execute();
        } catch (RiakException e) {
            throw new RuntimeException(String.format("could not delete mail '%s'", messageId), e);
        }

    }

    void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
        final long startedAt = System.currentTimeMillis();

        try {
            List<IndexEntry> indexEntries = fetchResult(mailBucket.fetchIndex(IntIndex.named(CREATED_INDEX)), time, maxResults);
            LOG.info("Cleanup: Mail ({} elements)", indexEntries.size());

            final int logInterval = logInterval(maxResults);
            final AtomicLong deleteCounter = new AtomicLong();

            new BlockingBatchExecutor<IndexEntry>("delete-mail", numCleanUpThreads, 2, TimeUnit.HOURS).executeAll(indexEntries, new Function<IndexEntry, Runnable>() {
                @Nullable
                @Override
                public Runnable apply(@Nullable final IndexEntry indexEntry) {
                    return new Runnable() {
                        @Override
                        public void run() {
                            try {
                                delete(indexEntry.getObjectKey());
                                long count = deleteCounter.incrementAndGet();
                                if (count % logInterval == 0) {
                                    long duration = new Duration(startedAt, System.currentTimeMillis()).getStandardSeconds();
                                    double rate = ((double) count) / ((double) (duration));
                                    LOG.info("Cleanup: Deleted {} mails in {}s. {}/s", count, duration, rate);
                                }
                            } catch (RuntimeException e) {
                                LOG.error("Cleanup: could not delete Mail: " + indexEntry, e);
                            }

                        }
                    };
                }
            });
            LOG.info("Cleanup: Deleted {} keys in the mail bucket", deleteCounter);

        } catch (RiakException e) {
            throw new RuntimeException(e);
        }

    }

    public Stream<String> streamMailIdsFrom(DateTime fromTime) {
        try {
            long endMin = timestampInMinutes(fromTime);
            LOG.debug("Streaming mail created from {} ({})", endMin, fromTime);

            Spliterator<IndexEntry> idxSplitterator =
                    mailBucket.fetchIndex(IntIndex.named(CREATED_INDEX))
                            .from(0)
                            .to(endMin)
                            .executeStreaming()
                            .spliterator();

            return StreamSupport.stream(idxSplitterator, false).map(idx -> idx.getObjectKey());
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }

    }

    public Stream<String> streamMailIdsCreatedBetween(DateTime fromTime, DateTime toTime) {
        try {

            long startMin = timestampInMinutes(fromTime);
            long endMin = timestampInMinutes(toTime);
            LOG.debug("Streaming mail created from {} ({}) to {} ({})", fromTime, startMin, toTime, endMin);

            Spliterator<IndexEntry> idxSpliterator =
                    mailBucket.fetchIndex(IntIndex.named(CREATED_INDEX))
                            .from(startMin)
                            .to(endMin)
                            .executeStreaming()
                            .spliterator();

            return StreamSupport.stream(idxSpliterator, false).map(idx -> idx.getObjectKey());
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }

    }


}
