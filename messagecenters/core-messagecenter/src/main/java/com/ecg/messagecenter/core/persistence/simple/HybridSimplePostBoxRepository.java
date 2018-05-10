package com.ecg.messagecenter.core.persistence.simple;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public PostBox byId(PostBoxId id) {
        PostBox postBoxInCassandra = cassandraRepository.byId(id);
        if (!postBoxInCassandra.getConversationThreads().isEmpty()) {
            if (deepMigrationEnabled) {
                return deepCompare(postBoxInCassandra);
            }
            return postBoxInCassandra;
        }

        PostBox postBoxInRiak = riakRepository.byId(id);
        if (postBoxInRiak.getConversationThreads().isEmpty()) {
            return postBoxInRiak;
        }

        // do normal one-time migration:
        migratePostBoxNecessaryCounter.inc();
        LOG.debug("Migrating PostBox {} with {} conversation threads from Riak to Cassandra", id.asString(), postBoxInRiak.getConversationThreads().size());

        // Essentially do a cross-cluster synchronize on this particular PostBox email to avoid duplication
        if (!migrationState.tryClaim(PostBox.class, id.asString())) {
            LOG.warn("Could not claim lock on email {}, not migrating PostBox", id.asString());
            return postBoxInRiak;
        }

        cassandraRepository.write(postBoxInRiak);
        migratePostBoxCounter.inc();
        migrateConversationThreadCounter.inc(postBoxInRiak.getConversationThreads().size());

        return postBoxInRiak;
    }

    @Override
    public PostBox byIdWithoutConversationThreads(PostBoxId id) {
        return byId(id);
    }

    private PostBox deepCompare(PostBox<AbstractConversationThread> postBoxInCassandra) {
        PostBoxId id = PostBoxId.fromEmail(postBoxInCassandra.getEmail());
        LOG.debug("Deep comparing Postbox {} in Riak and Cassandra", id);
        PostBox<AbstractConversationThread> postBoxInRiak = riakRepository.byId(id);

        if (!postBoxInRiak.getLastModification().isAfter(postBoxInCassandra.getLastModification()) &&
                postBoxInRiak.getConversationThreads().size() == postBoxInCassandra.getConversationThreads().size()) {
            LOG.debug("Postbox for email {} doesn't have a newer modificationDate in Riak ({}) than in Cassandra ({}) and the number of conversationThreads ({}) is equal, not migrating it",
                    id, postBoxInRiak.getLastModification(), postBoxInCassandra.getLastModification(), postBoxInRiak.getConversationThreads().size());
            return postBoxInRiak;
        }

        // Essentially do a cross-cluster synchronize on this particular PostBox email to avoid duplication
        if (!migrationState.tryClaim(PostBox.class, id.asString())) {
            LOG.warn("Could not claim lock on email {}, not deep migrating PostBox", id);
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
                LOG.debug("Found convThread to upsert: {}. Related Cassandra convThread: {}", r.getValue().fullToString(), c == null ? "null": c.fullToString());
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
                LOG.debug("Updating Postbox {} in Cassandra with convThread from Riak: {}", id, convThreadR.fullToString());
                cassandraRepository.writeThread(id, convThreadR);
            }

            // delete the removed convThreads from Cassandra
            if (postboxDeleteCthreadEnable && threadsIdsToDelete.size() > 0) {
                LOG.debug("Removing {} convThreads from Postbox {} in Cassandra", threadsIdsToDelete.size(), id);
                cassandraRepository.deleteConversations(postBoxInCassandra, threadsIdsToDelete);
            } else {
                LOG.debug("Extra {} convThreads from Postbox {} in Cassandra", threadsIdsToDelete.size(), id);
            }
        } catch (RuntimeException e) {
            LOG.error("Exception caught in deepcompare - {}", e);
        }

        LOG.debug("Done with deep comparing Postbox {} in Riak and Cassandra", id);
        return postBoxInRiak;
    }

    @Override
    public void write(PostBox postBox) {
        try {
            cassandraRepository.write(postBox);
        } finally {
            riakRepository.write(postBox);
        }
    }

    @Override
    public void deleteConversations(PostBoxId postBoxId, List<String> convIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConversations(PostBox postBox, List<String> deletedIds) {
        try {
            cassandraRepository.deleteConversations(postBox, deletedIds);
        } finally {
            riakRepository.deleteConversations(postBox, deletedIds);
        }
    }

    @Override
    public void markConversationsAsRead(PostBox postBox, List<AbstractConversationThread> conversations) {
        try {
            cassandraRepository.markConversationsAsReadForHybrid(postBox, conversations);
        } finally {
            riakRepository.markConversationsAsRead(postBox, conversations);
        }
    }

    @Override
    public boolean cleanup(DateTime time) {
        // cleanup() for the HybridSimplePostBoxRepository is only called from the RiakSimplePostBoxCleanupCronjob;
        // the CassandraSimplePostBoxCleanupCronJob goes straight to the CassandraSimplePostBoxRepository

        return riakRepository.cleanup(time);
    }

    @Override
    public Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId) {
        Optional<AbstractConversationThread> thread = cassandraRepository.threadById(id, conversationId);

        if (!thread.isPresent()) {
            thread = riakRepository.threadById(id, conversationId);

            if (thread.isPresent()) {
                LOG.debug("ConversationThread {} for postbox {} is in Riak but not in Cassandra, migrating the whole postbox", conversationId, id.asString());
                // byId makes sure to migrate the postbox
                byId(id);
            }
        }

        return thread;
    }

    @Override
    public Long upsertThread(PostBoxId id, AbstractConversationThread conversationThread, boolean markAsUnread) {
        try {
            Long unreadCount = cassandraRepository.upsertThread(id, conversationThread, markAsUnread);
            LOG.debug("Upserted convThread {} for postbox {} in Cassandra", conversationThread.getConversationId(), id.asString());
            return unreadCount;
        } catch (Exception e) {
            LOG.error("Could not upsert thread {} for postbox {} in Cassandra", conversationThread.fullToString(), id.asString(), e);
            throw e;
        } finally {
            riakRepository.upsertThread(id, conversationThread, markAsUnread);
            LOG.debug("Upserted convThread {} for postbox {} in Riak", conversationThread.getConversationId(), id.asString());
        }
    }

    @Override
    public int unreadCountInConversation(PostBoxId id, String conversationId) {
        return cassandraRepository.unreadCountInConversation(id, conversationId);
    }

    @Override
    public int unreadCountInConversations(PostBoxId id, List<AbstractConversationThread> conversations) {
        return cassandraRepository.unreadCountInConversations(id, conversations);
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
