package com.ecg.messagecenter.persistence.block;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HybridConversationBlockRepository implements ConversationBlockRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridConversationBlockRepository.class);

    private final Counter migrateConversationBlockCounter = TimingReports.newCounter("migration.migrate-conversation-block");

    private RiakConversationBlockRepository riakRepository;

    private CassandraConversationBlockRepository cassandraRepository;

    private HybridMigrationClusterState migrationState;

    public HybridConversationBlockRepository(RiakConversationBlockRepository riakRepository, CassandraConversationBlockRepository cassandraRepository, HybridMigrationClusterState migrationState) {
        this.riakRepository = riakRepository;
        this.cassandraRepository = cassandraRepository;
        this.migrationState = migrationState;
    }

    @Override
    public ConversationBlock byId(String conversationId) {
        ConversationBlock conversationBlock = cassandraRepository.byId(conversationId);

        if (conversationBlock == null) {
            conversationBlock = riakRepository.byId(conversationId);

            if (conversationBlock != null) {
                if (migrationState.tryClaim(ConversationBlock.class, conversationId)) {
                    LOG.debug("Migrating ConversationBlock for Conversation with id {} from Riak to Cassandra", conversationId);

                    cassandraRepository.write(conversationBlock);

                    migrateConversationBlockCounter.inc();
                }
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
        } catch (Exception e) {
            LOG.error("Error while performing Riak ConversationBlock cleanup", e);
        }

        try {
            cassandraRepository.cleanup(time);
        } catch (Exception e) {
            LOG.error("Error while performing Cassandra ConversationBlock cleanup", e);
        }
    }

    @Override
    public List<String> getIds() {
        // in hybrid mode Riak would have at least the same (or even bigger) dataset
        return riakRepository.getIds();
    }
}
