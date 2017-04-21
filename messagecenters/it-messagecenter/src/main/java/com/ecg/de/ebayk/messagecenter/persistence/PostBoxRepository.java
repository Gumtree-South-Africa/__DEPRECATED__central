package com.ecg.de.ebayk.messagecenter.persistence;

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
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class PostBoxRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxRepository.class);

    private final static Timer COMMIT_TIMER = TimingReports.newTimer("postBoxRepo-commit");
    private final static Timer GET_BY_ID_TIMER = TimingReports.newTimer("postBoxRepo-getById");
    private final static Timer DELETE_TIMER = TimingReports.newTimer("postBoxRepo-delete");

    static final String UPDATED_INDEX = "modifiedAt";
    static final String POST_BOX = "postbox";

    private final Bucket postBoxBucket;
    private final Converter<PostBox> converter;
    private final ConflictResolver<PostBox> resolver;

    PostBoxRepository(IRiakClient riakClient, int maxAgeDays) {
        this(riakClient, "", maxAgeDays);
    }

    @Autowired PostBoxRepository(IRiakClient riakClient,
                    @Value("${persistence.riak.bucket.name.prefix:}") String bucket_prefix,
                    @Value("${replyts.maxConversationAgeDays}") int maxAgeDays) {
        try {
            String bucketName = bucket_prefix + POST_BOX;
            this.postBoxBucket = riakClient.fetchBucket(bucketName).execute();
            this.converter = new PostBoxConverter(bucketName);
            this.resolver = new PostBoxConflictResolver();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    public PostBox byId(String email) {
        Timer.Context timerContext = GET_BY_ID_TIMER.time();
        try {

            LOG.info("Getting postbox by id: " + email);
            PostBox postBox = postBoxBucket.
                            fetch(email.toLowerCase(), PostBox.class).
                            withConverter(converter).
                            withResolver(resolver).
                            notFoundOK(true).
                            r(Quora.QUORUM).
                            withRetrier(new DefaultRetrier(3)).
                            execute();

            if (postBox == null) {
                postBox = new PostBox(email.toLowerCase(), Optional.of(0L),
                                Lists.<ConversationThread>newArrayList());
            }

            logPostBox(postBox);
            return postBox;

        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load post-box by email #" + email, e);
        } finally {
            timerContext.stop();
        }
    }

    private void logPostBox(PostBox postBox) {
        LOG.info("Getting postbox [" + postBox.getEmail() + "] size: " + postBox
                        .getConversationThreads().size());
    }

    public void write(PostBox postBox) {
        Timer.Context timerContext = COMMIT_TIMER.time();

        try {
            LOG.info("Writing postbox with mail: " + postBox.getEmail());
            logPostBox(postBox);
            postBoxBucket.store(postBox.getEmail(), postBox).withConverter(converter)
                            .withResolver(resolver).returnBody(false).w(Quora.QUORUM).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not write post-box #" + postBox.getEmail(), e);
        } finally {
            timerContext.stop();
        }
    }

    public void cleanupLongTimeUntouchedPostBoxes(DateTime time) {
        try {
            StreamingOperation<IndexEntry> keyStream =
                            postBoxBucket.fetchIndex(IntIndex.named(UPDATED_INDEX)).from(0)
                                            .to(time.getMillis()).executeStreaming();

            LOG.info("Removing num post-boxes... ");
            int counter = 0;
            for (IndexEntry indexEntry : keyStream) {
                try {
                    delete(indexEntry.getObjectKey());
                    if (counter % 1000 == 0) {
                        LOG.info("Iterated postbox to cleanup number: " + counter);
                    }
                } catch (RuntimeException e) {
                    LOG.error("Cleanup: could not cleanup postbox: " + indexEntry.getObjectKey(),
                                    e);
                } finally {
                    counter++;
                }
            }
            LOG.info("finished postbox cleanup overall deleted items: " + counter);
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }

    private void delete(String email) {
        Timer.Context context = DELETE_TIMER.time();
        LOG.info("Delete postbox with mail: " + email);
        try {
            postBoxBucket.delete(email).execute();
        } catch (RiakException e) {
            LOG.error("could not delete post box", e);
        } finally {
            context.stop();
        }
    }
}
