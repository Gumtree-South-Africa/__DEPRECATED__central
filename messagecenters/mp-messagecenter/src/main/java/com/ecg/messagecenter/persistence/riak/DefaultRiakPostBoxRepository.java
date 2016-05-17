package com.ecg.messagecenter.persistence.riak;

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
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

public class DefaultRiakPostBoxRepository implements RiakPostBoxRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRiakPostBoxRepository.class);

    private final static Timer WRITE_POSTBOX_TIMER = TimingReports.newTimer("postBoxRepo-commit");
    private final static Timer GET_POSTBOX_TIMER = TimingReports.newTimer("postBoxRepo-getById");
    private final static Timer DELETE_POSTBOX_TIMER = TimingReports.newTimer("postBoxRepo-delete");

    public static final String UPDATED_INDEX = "modifiedAt";
    public static final String POST_BOX = "postbox";

    private final Bucket postBoxBucket;
    private final Converter<PostBox> converter;
    private final ConflictResolver<PostBox> resolver;

    @Autowired
    public DefaultRiakPostBoxRepository(IRiakClient riakClient, String bucketNamePrefix) {
        try {
            this.postBoxBucket = riakClient.fetchBucket(StringUtils.hasLength(bucketNamePrefix) ? bucketNamePrefix + POST_BOX : POST_BOX).execute();
            this.converter = new PostBoxConverter();
            this.resolver = new PostBoxConflictResolver();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PostBox getPostBox(String email) {
        Timer.Context timerContext = GET_POSTBOX_TIMER.time();
        try {

            PostBox postBox = postBoxBucket.
                    fetch(email.toLowerCase(), PostBox.class).
                    withConverter(converter).
                    withResolver(resolver).
                    notFoundOK(true).
                    r(Quora.QUORUM).
                    withRetrier(new DefaultRetrier(3)).
                    execute();

            if (postBox == null) {
                postBox = new PostBox(email.toLowerCase(), Lists.<ConversationThread>newArrayList());
            }

            return postBox;

        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load post-box by email #" + email, e);
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public void write(PostBox postBox) {
        Timer.Context timerContext = WRITE_POSTBOX_TIMER.time();

        try {
            postBoxBucket.store(postBox.getUserId(), postBox)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .withMutator(new RiakPostBoxMutator(postBox, postBox.flushRemovedThreads()))
                    .returnBody(false)
                    .w(Quora.QUORUM)
                    .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not write post-box #" + postBox.getUserId(), e);
        } finally {
            timerContext.stop();
        }
    }

    @Override
    public void cleanupLongTimeUntouchedPostBoxes(DateTime time) {
        try {
            StreamingOperation<IndexEntry> keyStream = postBoxBucket.fetchIndex(IntIndex.named(UPDATED_INDEX))
                    .from(0)
                    .to(time.getMillis())
                    .executeStreaming();

            LOGGER.info("Removing num post-boxes... ");
            int counter = 0;
            for (IndexEntry indexEntry : keyStream) {
                try {
                    delete(indexEntry.getObjectKey());
                    if (counter % 1000 == 0) {
                        LOGGER.info("Iterated postbox to cleanup number: " + counter);
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Cleanup: could not cleanup postbox: " + indexEntry.getObjectKey(), e);
                } finally {
                    counter++;
                }
            }
            LOGGER.info("finished postbox cleanup overall deleted items: " + counter);
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ConversationThread> getConversationThread(String postBoxId, String conversationId) {
        PostBox postBox = getPostBox(postBoxId);
        return postBox.lookupConversation(conversationId);
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String postBoxId) {
        PostBox postBox = getPostBox(postBoxId);
        int numUnreadMessages = postBox.getNewRepliesCounter();
        int numUnreadConversations = postBox.getNumUnreadConversations();
        return new PostBoxUnreadCounts(numUnreadConversations, numUnreadMessages);
    }

    private void delete(String postBoxId) {
        Timer.Context context = DELETE_POSTBOX_TIMER.time();
        try {
            postBoxBucket.delete(postBoxId).execute();
        } catch (RiakException e) {
            LOGGER.error("could not delete post box", e);
        } finally {
            context.stop();
        }
    }
}