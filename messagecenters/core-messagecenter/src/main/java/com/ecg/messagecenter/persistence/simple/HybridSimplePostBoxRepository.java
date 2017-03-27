package com.ecg.messagecenter.persistence.simple;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class HybridSimplePostBoxRepository implements RiakSimplePostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridSimplePostBoxRepository.class);

    public final Counter migratePostBoxCounter = TimingReports.newCounter("migration.migrate-postbox");
    public final Counter migratePostBoxNecessaryCounter = TimingReports.newCounter("migration.migrate-postbox-necessary");
    public final Counter migrateConversationThreadCounter = TimingReports.newCounter("migration.migrate-thread");

    private RiakSimplePostBoxRepository riakRepository;

    private CassandraSimplePostBoxRepository cassandraRepository;

    private HybridMigrationClusterState migrationState;

    public HybridSimplePostBoxRepository(RiakSimplePostBoxRepository riakRepository, CassandraSimplePostBoxRepository cassandraRepository, HybridMigrationClusterState migrationState) {
        this.riakRepository = riakRepository;
        this.cassandraRepository = cassandraRepository;
        this.migrationState = migrationState;
    }

    @Override
    public PostBox byId(String email) {
        PostBox postBox = cassandraRepository.byId(email);

        if (postBox.getConversationThreads().isEmpty()) {
            postBox = riakRepository.byId(email);

            if (!postBox.getConversationThreads().isEmpty()) {
                migratePostBoxNecessaryCounter.inc();

                // Essentially do a cross-cluster synchronize on this particular PostBox email to avoid duplication
                if (migrationState.tryClaim(PostBox.class, email)) {
                    LOG.debug("Migrating PostBox {}, with {} conversation threads from Riak to Cassandra", email, postBox.getConversationThreads().size());

                    cassandraRepository.write(postBox);

                    migratePostBoxCounter.inc();
                }
            }

            return postBox;
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
        // cleanup() for the HybridSimplePostBoxRepository is only called from the RiakSimplePostBoxCleanupCronjob;
        // the CassandraSimplePostBoxCleanupCronJob goes straight to the CassandraSimplePostBoxRepository

        riakRepository.cleanup(time);
    }

    @Override
    public Optional<AbstractConversationThread> threadById(String email, String conversationId) {
        Optional<AbstractConversationThread> thread = cassandraRepository.threadById(email, conversationId);

        if (!thread.isPresent()) {
            thread = riakRepository.threadById(email, conversationId);

            if (thread.isPresent()) {
                // Essentially do a cross-cluster synchronize on this particular ConversationThread to avoid duplication

                if (migrationState.tryClaim(thread.get().getClass(), "[" + email + "]" + conversationId)) {
                    LOG.debug("Migrating ConversationThread {}/{} from Riak to Cassandra", email, conversationId);

                    cassandraRepository.writeThread(email, thread.get());

                    migrateConversationThreadCounter.inc();
                }
            }
        }

        return thread;
    }

    @Override
    public Long upsertThread(String email, AbstractConversationThread conversationThread, boolean markAsUnread) {
        try {
            return cassandraRepository.upsertThread(email, conversationThread, markAsUnread);
        } finally {
            riakRepository.upsertThread(email, conversationThread, markAsUnread);
        }
    }

    @Override
    public long getMessagesCount(DateTime fromDate, DateTime toDate) {
        return riakRepository.getMessagesCount(fromDate, toDate);
    }

    @Override
    public Stream<String> streamPostBoxIds(DateTime fromDate, DateTime toDate) { // use endDate as its current date
        return riakRepository.streamPostBoxIds(fromDate, toDate);
    }

    @Override
    public List<String> getPostBoxIds(DateTime fromDate, DateTime toDate) {
        return riakRepository.getPostBoxIds(fromDate, toDate);
    }
}
