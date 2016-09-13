package com.ecg.comaas.r2cmigration.difftool.repo;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.ConflictResolver;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.comaas.r2cmigration.difftool.DiffToolConfiguration;
import com.ecg.messagecenter.persistence.simple.*;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;


import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;

import static com.ecg.comaas.r2cmigration.difftool.DiffToolConfiguration.*;

@Repository
public class RiakPostboxRepo {

    private static final Logger LOG = LoggerFactory.getLogger(RiakPostboxRepo.class);

    private static final Timer GET_BY_ID_RIAK_TIMER = TimingReports.newTimer("difftool.riak-postbox-getById");

    private IRiakClient riakClient;

    @Autowired
    private Converter<PostBox> converter;
    @Autowired
    private ConflictResolver<PostBox> resolver;

    @Value("${replyts.maxConversationAgeDays:360}")
    private int maxAgeDays;

    private Bucket postbox;

    @Autowired
    public RiakPostboxRepo(IRiakClient riakClient) {
        this.riakClient = riakClient;
        try {
            this.postbox = riakClient.fetchBucket(RIAK_POSTBOX_BUCKET_NAME).execute();
        } catch (RiakException re) {
            throw new RuntimeException(re);
        }
    }


    public long getMessagesCount(DateTime fromDate, DateTime toDate) throws RiakRetryFailedException {
        AtomicLong counter = new AtomicLong();
        streamPostBoxIds(fromDate, toDate).forEach(c -> counter.getAndIncrement());
        return counter.get();
    }


    @PreDestroy
    void close() {
        riakClient.shutdown();
    }


    public PostBox getById(String email) {
        try (Timer.Context ignored = GET_BY_ID_RIAK_TIMER.time()) {
            return postbox
                    .fetch(email.toLowerCase(), PostBox.class)
                    .withConverter(converter)
                    .withResolver(resolver)
                    .notFoundOK(false)
                    .r(Quora.QUORUM)
                    .withRetrier(new DefaultRetrier(3))
                    .execute();

        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load post-box by email #" + email, e);
        }
    }

    public StreamingOperation<IndexEntry> streamPostBoxIds(DateTime fromDate, DateTime toDate) { // use endDate as its current date
        try {
            return postbox.fetchIndex(IntIndex.named(DiffToolConfiguration.RIAK_SECONDARY_INDEX_MODIFIED_AT))
                    .from(fromDate.getMillis())
                    .to(toDate.getMillis())
                    .executeStreaming();
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }

}
