package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.query.StreamingOperation;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class HybridSimplePostBoxRepository implements SimplePostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridSimplePostBoxRepository.class);

    private SimplePostBoxRepository riakRepository;

    private CassandraSimplePostBoxRepository cassandraRepository;

    public HybridSimplePostBoxRepository(SimplePostBoxRepository riakRepository, CassandraSimplePostBoxRepository cassandraRepository) {
        this.riakRepository = riakRepository;
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    public PostBox byId(String email) {
        PostBox postBox = cassandraRepository.byId(email);

        if (postBox == null) {
            postBox = riakRepository.byId(email);

            if (postBox != null) {
                cassandraRepository.write(postBox);
            }
        }

        return postBox;
    }

    @Override
    public void write(PostBox postBox) {
        write(postBox, Collections.emptyList());
    }

    @Override
    public void write(PostBox postBox, List<String> deletedIds) {
        try {
            cassandraRepository.write(postBox, deletedIds);
        } finally {
            riakRepository.write(postBox, deletedIds);
        }
    }

    @Override
    public void cleanup(DateTime time) {
        try {
            riakRepository.cleanup(time);
        } catch (RuntimeException e) {
            LOG.error("Error while performing Riak PostBox cleanup", e);
        }

        try {
            cassandraRepository.cleanup(time);
        } catch (RuntimeException e) {
            LOG.error("Error while performing Cassandra PostBox/ConversationThread cleanup", e);
        }
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
