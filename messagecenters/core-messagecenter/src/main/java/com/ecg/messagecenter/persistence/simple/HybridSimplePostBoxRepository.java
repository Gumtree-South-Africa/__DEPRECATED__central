package com.ecg.messagecenter.persistence.simple;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;


public class HybridSimplePostBoxRepository implements RiakSimplePostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridSimplePostBoxRepository.class);

    private final Counter migratePostBoxCounter = TimingReports.newCounter("migration.migrate-postbox");
    private final Counter migratePostBoxNecessaryCounter = TimingReports.newCounter("migration.migrate-postbox-necessary");
    private final Counter migrateConversationThreadCounter = TimingReports.newCounter("migration.migrate-thread");
    private final Timer deepMigrationTimer = TimingReports.newTimer("migration.deep-migrate-postbox");

    private final RiakSimplePostBoxRepository riakRepository;

    private final CassandraSimplePostBoxRepository cassandraRepository;

    private final HybridMigrationClusterState migrationState;
    private final boolean deepMigrationEnabled;
    private final boolean postboxDeleteCthreadEnable;

    public HybridSimplePostBoxRepository(RiakSimplePostBoxRepository riakRepository, CassandraSimplePostBoxRepository cassandraRepository,
                                         HybridMigrationClusterState migrationState, boolean deepMigrationEnabled, boolean postboxDeleteCthreadEnable) {
        this.riakRepository = riakRepository;
        this.cassandraRepository = cassandraRepository;
        this.migrationState = migrationState;
        this.deepMigrationEnabled = deepMigrationEnabled;
        this.postboxDeleteCthreadEnable = postboxDeleteCthreadEnable;
    }

    @Override
    public PostBox byId(String email) {
        PostBox postBoxInCassandra = cassandraRepository.byId(email);
        if (!postBoxInCassandra.getConversationThreads().isEmpty()) {
            if (deepMigrationEnabled) {
                return deepCompare(postBoxInCassandra);
            }
            return postBoxInCassandra;
        }

        PostBox postBoxInRiak = riakRepository.byId(email);
        if (postBoxInRiak.getConversationThreads().isEmpty()) {
            return postBoxInRiak;
        }

        // do normal one-time migration:
        migratePostBoxNecessaryCounter.inc();
        LOG.debug("Migrating PostBox {} with {} conversation threads from Riak to Cassandra", email, postBoxInRiak.getConversationThreads().size());

        // Essentially do a cross-cluster synchronize on this particular PostBox email to avoid duplication
        if (!migrationState.tryClaim(PostBox.class, email)) {
            LOG.warn("Could not claim lock on email {}, not migrating PostBox", email);
            return postBoxInRiak;
        }

        cassandraRepository.write(postBoxInRiak);
        migratePostBoxCounter.inc();
        migrateConversationThreadCounter.inc(postBoxInRiak.getConversationThreads().size());

        return postBoxInRiak;
    }

    private PostBox deepCompare(PostBox<AbstractConversationThread> postBoxInCassandra) {
        String email = postBoxInCassandra.getEmail();
        LOG.debug("Deep comparing Postbox {} in Riak and Cassandra", email);
        PostBox<AbstractConversationThread> postBoxInRiak = riakRepository.byId(email);

        if (!postBoxInRiak.getLastModification().isAfter(postBoxInCassandra.getLastModification()) &&
                postBoxInRiak.getConversationThreads().size() == postBoxInCassandra.getConversationThreads().size()) {
            LOG.debug("Postbox for email {} doesn't have a newer modificationDate in Riak ({}) than in Cassandra ({}) and the number of conversationThreads ({}) is equal, not migrating it",
                    email, postBoxInRiak.getLastModification(), postBoxInCassandra.getLastModification(), postBoxInRiak.getConversationThreads().size());
            return postBoxInRiak;
        }

        // Essentially do a cross-cluster synchronize on this particular PostBox email to avoid duplication
        if (!migrationState.tryClaim(PostBox.class, email)) {
            LOG.warn("Could not claim lock on email {}, not deep migrating PostBox", email);
            return postBoxInRiak;
        }

        try (Timer.Context t = deepMigrationTimer.time()) {
            List<AbstractConversationThread> threadsToUpsert = new ArrayList<>();
            List<String> threadsIdsToDelete = new ArrayList<>();

            Map<String, AbstractConversationThread> riak = Maps.newHashMap();
            for (AbstractConversationThread th : postBoxInRiak.getConversationThreads()) {
                riak.put(th.getConversationId(), th);
            }
            Map<String, AbstractConversationThread> cassandra = Maps.newHashMap();
            for (AbstractConversationThread th : postBoxInCassandra.getConversationThreads()) {
                cassandra.put(th.getConversationId(), th);
            }

            // find threads to upsert
            for (Map.Entry<String, AbstractConversationThread> r : riak.entrySet()) {
                AbstractConversationThread c = cassandra.get(r.getKey());
                if (c != null && !r.getValue().getModifiedAt().isAfter(c.getModifiedAt())) {
                    continue;
                }
                threadsToUpsert.add(r.getValue());
            }

            // find threads to delete
            for (String cid : cassandra.keySet()) {
                if (!riak.containsKey(cid)) {
                    threadsIdsToDelete.add(cid);
                }
            }

            // save the convThreads from Riak to Cassandra
            for (AbstractConversationThread convThreadR : threadsToUpsert) {
                LOG.debug("Updating Postbox {} in Cassandra with convThread from Riak: {}", email, convThreadR.fullToString());
                cassandraRepository.writeThread(email, convThreadR);
            }

            // delete the removed convThreads from Cassandra
            if (postboxDeleteCthreadEnable && threadsIdsToDelete.size() > 0) {
                LOG.debug("Removing {} convThreads from Postbox {} in Cassandra", threadsIdsToDelete.size(), email);
                cassandraRepository.deleteConversationThreads(email, threadsIdsToDelete);
            } else {
                LOG.debug("Extra {} convThreads from Postbox {} in Cassandra", threadsIdsToDelete.size(), email);
            }
        } catch (RuntimeException e) {
            LOG.error("Exception caught in deepcompare - {}", e);
        }

        LOG.debug("Done with deep comparing Postbox {} in Riak and Cassandra", email);
        return postBoxInRiak;
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
                LOG.debug("ConversationThread {} for postbox {} is in Riak but not in Cassandra, migrating the whole postbox");
                // byId makes sure to migrate the postbox
                byId(email);
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
