package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.ConflictResolver;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.basho.riak.client.convert.Converter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.basho.riak.client.query.indexes.IntIndex;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RiakReadOnlySimplePostBoxRepository implements RiakSimplePostBoxRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RiakReadOnlySimplePostBoxRepository.class);

    private final static Timer GET_BY_ID_TIMER = TimingReports.newTimer("postBoxRepo-getById");

    public static final String UPDATED_INDEX = "modifiedAt";
    public static final String POST_BOX = "postbox";

    @Autowired
    private Converter<PostBox> converter;

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private ConflictResolver<PostBox> resolver;

    @Value("${persistence.simple.bucket.name.prefix:}" + POST_BOX)
    private String bucketName;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    protected Bucket postBoxBucket;

    @PostConstruct
    public void createBucketProxy() {
        try {
            this.postBoxBucket = riakClient.fetchBucket(bucketName).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PostBox byId(PostBoxId id) {
        try (Timer.Context ignored = GET_BY_ID_TIMER.time()) {
            PostBox postBox = postBoxBucket
                    .fetch(id.asString(), PostBox.class)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .notFoundOK(true)
                    .r(Quora.QUORUM)
                    .withRetrier(new DefaultRetrier(3))
                    .execute();

            if (postBox == null) {
                postBox = new PostBox(id.asString(), Optional.of(0L), Lists.newArrayList(), maxAgeDays);
            }
            return postBox;
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load post-box by id #" + id.asString(), e);
        }
    }

    @Override
    public void write(PostBox postBox) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.write was called");
    }

    @Override
    public void write(PostBox postBox, List<String> deletedIds) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.write was called");
    }

    @Override
    public void cleanup(DateTime time) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.cleanup was called");
    }

    @Override
    public Long upsertThread(PostBoxId id, AbstractConversationThread conversationThread, boolean markAsUnread) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.upsertThread was called");

        // Don't update but do retrieve the existing PostBox in order to return the correct number of unread threads

        PostBox<AbstractConversationThread> existingPostBox = byId(id);

        return existingPostBox.getNewRepliesCounter().getValue();
    }

    @Override
    public Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId) {
        PostBox postBox = byId(id);

        return postBox.lookupConversation(conversationId);
    }

    @Override
    public Stream<String> streamPostBoxIds(DateTime fromDate, DateTime toDate) { // use endDate as its current date
        LOG.debug("Fetching Postboxes modifiedBetween {} - {}", fromDate, toDate);
        try {
            Spliterator<IndexEntry> idxSplitterator = postBoxBucket.fetchIndex(IntIndex.named(UPDATED_INDEX))
                    .from(fromDate.getMillis())
                    .to(toDate.getMillis())
                    .executeStreaming()
                    .spliterator();

            return StreamSupport.stream(idxSplitterator, false).map(idx -> idx.getObjectKey());

        } catch (RiakException e) {

            String errMess = "Streaming postboxes modified between '" + fromDate + "' and '" + toDate + "' failed";
            LOG.error(errMess, e);
            throw new RuntimeException(errMess, e);
        }
    }

    @Override
    public long getMessagesCount(DateTime fromDate, DateTime toDate) {
        return streamPostBoxIds(fromDate, toDate).count();
    }

    @Override
    public List<String> getPostBoxIds(DateTime fromDate, DateTime toDate) {
        return streamPostBoxIds(fromDate, toDate).collect(Collectors.toList());
    }

}