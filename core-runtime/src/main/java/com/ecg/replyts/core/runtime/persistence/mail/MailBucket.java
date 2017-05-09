package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.api.util.StorageSize;
import com.ecg.replyts.core.api.util.StorageUnit;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.mail.Chunks.Chunk;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

class MailBucket {


    private static final Histogram MAIL_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-mail-sizes.bytes");
    private static final Histogram CHUNK_COUNT_HISTOGRAM = TimingReports.newHistogram("riak-mail-chunks");


    static final String REPLYTS_MAIL_COMPLETE = "replyts/mail-complete";
    static final String REPLYTS_MAIL_MANIFEST = "replyts/mail-manifest";
    static final String REPLYTS_MAIL_CHUNK = "replyts/mail-chunk";

    private static final String MAIL_BUCKET = "mail";

    private final StorageSize chunkSize;
    private final BucketOpDelegate bucketOpDelegate;

    MailBucket(String bucketPrefix, IRiakClient client) {
        this(new BucketOpDelegate(bucketPrefix + MAIL_BUCKET, client), new StorageSize(1, StorageUnit.MB));
    }

    MailBucket(BucketOpDelegate bucketOpDelegate, StorageSize chunkSize) {
        this.chunkSize = chunkSize;
        this.bucketOpDelegate = bucketOpDelegate;

    }

    void persistMail(DateTime messageCreationDate, String messageId, byte[] compressedData) {
        StorageSize compressedDataSize = new StorageSize(compressedData.length, StorageUnit.B);
        MAIL_SIZE_HISTOGRAM.update(compressedData.length);

        if (saveChunked(compressedDataSize)) {
            Chunks chunks = new Chunks(compressedData, chunkSize);
            List<String> chunkKeys = Lists.newArrayList();
            for (Chunk chunk : chunks) {
                chunkKeys.add(bucketOpDelegate.storeOneChunk(messageCreationDate, messageId, chunk));
            }
            bucketOpDelegate.storeManifest(messageCreationDate, messageId, new Manifest(chunkKeys));

            CHUNK_COUNT_HISTOGRAM.update(chunks.count());

        } else {
            bucketOpDelegate.storeMailAsSingleObject(messageCreationDate, messageId, compressedData);
            CHUNK_COUNT_HISTOGRAM.update(1);

        }
    }

    private boolean saveChunked(StorageSize compressedDataSize) {
        return compressedDataSize.isBiggerThan(chunkSize);
    }

    Optional<byte[]> load(String messageId) {
        try {
            IRiakObject result = bucketOpDelegate.fetchKey(messageId);

            if (result == null) {
                return Optional.empty();
            }

            boolean isManifest = isManifest(result);
            if (isManifest) {
                List<String> chunkKeys = Manifest.parse(result.getValueAsString()).getChunkKeys();
                return Optional.of(join(chunkKeys));
            } else {
                return Optional.of(result.getValue());
            }

        } catch (RuntimeException e) {
            throw new RuntimeException(format("Could not fetch mail with messageId %s: ", messageId), e);
        }
    }

    public void delete(String messageId) {

        IRiakObject mailOrManifest = bucketOpDelegate.fetchKey(messageId);
        if (mailOrManifest != null && isManifest(mailOrManifest)) {
            List<String> chunkKeys = Manifest.parse(mailOrManifest.getValueAsString()).getChunkKeys();
            for (String chunkKey : chunkKeys) {
                bucketOpDelegate.delete(chunkKey);
            }

        }
        bucketOpDelegate.delete(messageId);
    }

    void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads) {
        bucketOpDelegate.deleteMailsByOlderThan(time, maxResults, numCleanUpThreads);
    }

    private boolean isManifest(IRiakObject result) {
        String resultContentType = Optional.ofNullable(result.getContentType()).orElse(REPLYTS_MAIL_COMPLETE);
        return resultContentType.startsWith(REPLYTS_MAIL_MANIFEST);
    }

    private byte[] join(List<String> chunkKeys) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (String key : chunkKeys) {
            try {
                os.write(bucketOpDelegate.fetchChunk(key));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return os.toByteArray();
    }

    public Stream<String> streamMailIdsSince(DateTime fromTime) {
       return bucketOpDelegate.streamMailIdsFrom(fromTime);
    }

    public Stream<String> streamMailIdsCreatedBetween(DateTime fromTime, DateTime toTime) {
        return bucketOpDelegate.streamMailIdsCreatedBetween(fromTime, toTime);
    }
}
