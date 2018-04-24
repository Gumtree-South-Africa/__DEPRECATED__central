package com.ecg.messagecenter.kjca.persistence.block;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.ConflictResolver;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.List;

public class RiakConversationBlockRepository implements ConversationBlockRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RiakConversationBlockRepository.class);

    private static final Timer COMMIT_TIMER = TimingReports.newTimer("convoBlockRepo-commit");
    private static final Timer GET_BY_ID_TIMER = TimingReports.newTimer("convoBlockRepo-getById");
    private static final Timer DELETE_TIMER = TimingReports.newTimer("convoBlockRepo-delete");
    private static final Timer GET_IDS_TIMER = TimingReports.newTimer("convoBlockRepo-getIds");

    public static final String BUCKET_NAME = "conversation_block";
    public static final String CREATED_INDEX = "createdAt";

    @Autowired
    private Converter<ConversationBlock> converter;

    @Autowired
    private ConflictResolver<ConversationBlock> resolver;

    @Autowired
    private IRiakClient riakClient;

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String bucketPrefix;

    private Bucket bucket;

    @PostConstruct
    public void initializeBucket() {
        String bucketName = bucketPrefix + BUCKET_NAME;

        try {
            this.bucket = riakClient.fetchBucket(bucketName).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConversationBlock byId(String conversationId) {
        try (Timer.Context ignored = GET_BY_ID_TIMER.time()) {
            return bucket.fetch(conversationId, ConversationBlock.class)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .notFoundOK(true)
                    .r(Quora.QUORUM)
                    .withRetrier(new DefaultRetrier(3))
                    .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("Could not load conversation block object by conversation id #"
                    + conversationId, e);
        }
    }

    @Override
    public void write(ConversationBlock conversationBlock) {
        try (Timer.Context ignored = COMMIT_TIMER.time()) {
            bucket.store(conversationBlock.getConversationId(), conversationBlock)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .returnBody(false)
                    .w(Quora.QUORUM)
                    .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("Could not write conversation block object with conversation id #"
                    + conversationBlock.getConversationId(), e);
        }
    }

    @Override
    public void cleanup(DateTime deleteBlocksBefore) {
        try {
            LOG.info("Beginning to remove user conversation blocks");

            StreamingOperation<IndexEntry> keyStream = bucket.fetchIndex(IntIndex.named(CREATED_INDEX))
                    .from(0)
                    .to(deleteBlocksBefore.getMillis())
                    .executeStreaming();

            int counter = 0;
            for (IndexEntry indexEntry : keyStream) {
                try (Timer.Context ignored = DELETE_TIMER.time()) {
                    bucket.delete(indexEntry.getObjectKey()).execute();
                    if (counter % 1000 == 0) {
                        LOG.info("Cleaned up {} user conversation blocks", counter);
                    }
                } catch (RiakException e) {
                    LOG.error("Could not delete user conversation block for conversation [{}]", indexEntry.getObjectKey(), e);
                } finally {
                    counter++;
                }
            }
            LOG.info("Finished user conversation block cleanup. Total deleted items: {}", counter);
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getIds() {
        try (Timer.Context ignored = GET_IDS_TIMER.time()) {
            return bucket.keys().getAll();
        } catch (RiakException e) {
            throw new RuntimeException("Getting all conversationBlock IDs failed", e);
        }
    }
}
