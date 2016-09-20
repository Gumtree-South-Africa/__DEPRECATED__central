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
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
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
import java.util.concurrent.atomic.AtomicLong;

public class RiakReadOnlySimplePostBoxRepository implements SimplePostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RiakReadOnlySimplePostBoxRepository.class);

    private final static Timer GET_BY_ID_TIMER = TimingReports.newTimer("postBoxRepo-getById");

    public static final String POST_BOX = "postbox";

    @Autowired
    private Converter<PostBox> converter;

    @Autowired
    private ConflictResolver<PostBox> resolver;

    @Autowired
    private RiakSimplePostBoxMerger postBoxMerger;

    @Autowired
    private IRiakClient riakClient;

    @Value("${persistence.simple.bucket.name.prefix:}" + POST_BOX)
    private String bucketName;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    private Bucket postBoxBucket;

    @PostConstruct
    public void createBucket() {
        try {
            this.postBoxBucket = riakClient.fetchBucket(bucketName).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PostBox byId(String email) {
        Timer.Context timerContext = GET_BY_ID_TIMER.time();

        try {
            PostBox postBox = postBoxBucket
              .fetch(email.toLowerCase(), PostBox.class)
              .withConverter(converter)
              .withResolver(resolver)
              .notFoundOK(true)
              .r(Quora.QUORUM)
              .withRetrier(new DefaultRetrier(3))
              .execute();

            if (postBox == null) {
                postBox = new PostBox(email.toLowerCase(), Optional.of(0L), Lists.newArrayList(), maxAgeDays);
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
    public long getMessagesCount(DateTime fromDate, DateTime toDate) {
        AtomicLong counter = new AtomicLong();
        streamPostBoxIds(fromDate, toDate).forEach(c -> counter.getAndIncrement());
        return counter.get();
    }

    @Override
    public StreamingOperation<IndexEntry> streamPostBoxIds(DateTime fromDate, DateTime toDate) { // use endDate as its current date
        try {
            return postBoxBucket.fetchIndex(IntIndex.named(RiakSimplePostBoxRepository.UPDATED_INDEX))
                    .from(fromDate.getMillis())
                    .to(toDate.getMillis())
                    .executeStreaming();
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getPostBoxIds(DateTime fromDate, DateTime toDate) {
        List<String> postboxids = Lists.newArrayList();
        streamPostBoxIds(fromDate, toDate).forEach(id -> postboxids.add(id.getObjectKey()));
        return postboxids;
    }
}