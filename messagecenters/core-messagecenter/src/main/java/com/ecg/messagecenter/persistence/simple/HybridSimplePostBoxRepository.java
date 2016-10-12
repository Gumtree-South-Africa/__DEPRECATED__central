package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class HybridSimplePostBoxRepository implements RiakSimplePostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridSimplePostBoxRepository.class);

    public final Counter TOBE_MIGRATED_POSTBOX_COUNTER = TimingReports.newCounter("migration.tobe-migrated-riak-postbox-counter");
    public final Counter MIGRATED_POSTBOX_COUNTER = TimingReports.newCounter("migration.migrated-postbox-counter");

    private RiakSimplePostBoxRepository riakRepository;

    private CassandraSimplePostBoxRepository cassandraRepository;

    public HybridSimplePostBoxRepository(RiakSimplePostBoxRepository riakRepository, CassandraSimplePostBoxRepository cassandraRepository) {
        this.riakRepository = riakRepository;
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    public PostBox byId(String email) {
        LOG.debug("Fetching postbox byId {}", email);

        synchronized (cassandraRepository) {
            PostBox postBox = cassandraRepository.byId(email);

            if (postBox == null) {
                postBox = riakRepository.byId(email);
                LOG.debug("Postbox {} found in riak only", email);

                if (postBox != null) {
                    TOBE_MIGRATED_POSTBOX_COUNTER.inc();

                    if (!postBox.getConversationThreads().isEmpty()) {
                        LOG.debug("Migrating Postbox {} from riak to cassandra", email);
                        cassandraRepository.write(postBox);
                        MIGRATED_POSTBOX_COUNTER.inc();
                    } else {
                        LOG.debug("Postbox id {} is empty skipping", email);
                    }

                } else {
                    LOG.warn("Postbox id {} not found in known repositories", email);
                }
            }
            return postBox;
        }
    }

    @Override
    public void write(PostBox postBox) {
        write(postBox, Collections.emptyList());
    }

    @Override
    public void write(PostBox postBox, List<String> deletedIds) {
        synchronized (cassandraRepository) {
            try {
                cassandraRepository.write(postBox, deletedIds);
            } finally {
                riakRepository.write(postBox, deletedIds);
            }
        }
    }

    @Override
    public void cleanup(DateTime time) {
        riakRepository.cleanup(time);
    }

    @Override
    public long getMessagesCount(DateTime fromDate, DateTime toDate) {
        return riakRepository.getMessagesCount(fromDate, toDate);
    }

    @Override
    public StreamingOperation<IndexEntry> streamPostBoxIds(DateTime fromDate, DateTime toDate) { // use endDate as its current date
        return riakRepository.streamPostBoxIds(fromDate, toDate);
    }

    @Override
    public List<String> getPostBoxIds(DateTime fromDate, DateTime toDate) {
        return riakRepository.getPostBoxIds(fromDate, toDate);
    }

}
