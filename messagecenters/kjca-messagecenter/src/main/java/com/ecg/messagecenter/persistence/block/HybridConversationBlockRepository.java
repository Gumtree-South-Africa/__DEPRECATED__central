package com.ecg.messagecenter.persistence.block;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HybridConversationBlockRepository implements ConversationBlockRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridConversationBlockRepository.class);

    private RiakConversationBlockRepository riakRepository;

    private CassandraConversationBlockRepository cassandraRepository;

    public HybridConversationBlockRepository(RiakConversationBlockRepository riakRepository, CassandraConversationBlockRepository cassandraRepository) {
        this.riakRepository = riakRepository;
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    public ConversationBlock byId(String conversationId) {
        ConversationBlock conversationBlock = cassandraRepository.byId(conversationId);

        if (conversationBlock == null) {
            conversationBlock = riakRepository.byId(conversationId);

            if (conversationBlock != null) {
                cassandraRepository.write(conversationBlock);
            }
        }

        return conversationBlock;
    }

    @Override
    public void write(ConversationBlock conversationBlock) {
        try {
            cassandraRepository.write(conversationBlock);
        } finally {
            riakRepository.write(conversationBlock);
        }
    }

    @Override
    public void cleanup(DateTime time) {
        try {
            riakRepository.cleanup(time);
        } catch (RuntimeException e) {
            LOG.error("Error while performing Riak ConversationBlock cleanup", e);
        }

        try {
            cassandraRepository.cleanup(time);
        } catch (RuntimeException e) {
            LOG.error("Error while performing Cassandra ConversationBlock cleanup", e);
        }
    }
}
