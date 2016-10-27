package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class HybridConversationRepository implements MutableConversationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridConversationRepository.class);

    private final Counter migrateConversationCounter = TimingReports.newCounter("migration.migrate-conversation");
    private final Timer migrationConversationLagTimer = TimingReports.newTimer("migration.migrate-conversation-lag");

    private CassandraConversationRepository cassandraConversationRepository;

    private RiakConversationRepository riakConversationRepository;

    private HybridMigrationClusterState migrationState;

    public HybridConversationRepository(CassandraConversationRepository cassandraConversationRepository, RiakConversationRepository riakConversationRepository, HybridMigrationClusterState migrationState) {
        this.cassandraConversationRepository = cassandraConversationRepository;
        this.riakConversationRepository = riakConversationRepository;
        this.migrationState = migrationState;
    }

    @Override
    public MutableConversation getById(String conversationId) {
        MutableConversation conversation = cassandraConversationRepository.getById(conversationId);

        if (conversation == null) {
             conversation = riakConversationRepository.getById(conversationId);

            if (conversation != null) {
                migrateEventsToCassandra(conversationId);
            }
        }

        return conversation;
    }

    @Override
    public MutableConversation getBySecret(String secret) {
        MutableConversation conversation = cassandraConversationRepository.getBySecret(secret);

        if (conversation == null) {
            conversation = riakConversationRepository.getBySecret(secret);

            if (conversation != null) {
                migrateEventsToCassandra(conversation.getId());
            }
        }

        return conversation;
    }

    @Override
    public boolean isSecretAvailable(String secret) {
        return riakConversationRepository.isSecretAvailable(secret) || cassandraConversationRepository.isSecretAvailable(secret);
    }

    @Override
    public List<String> listConversationsModifiedBetween(DateTime start, DateTime end) {
        // Defer to Riak as the single source of truth; once both are in sync, switch to persistence.strategy = cassandra
        return riakConversationRepository.listConversationsModifiedBetween(start, end);
    }

    @Override
    public Stream<String> streamConversationsModifiedBetween(DateTime start, DateTime end) {
        // Defer to Riak as the single source of truth; once both are in sync, switch to persistence.strategy = cassandra
        return riakConversationRepository.streamConversationsModifiedBetween(start, end);
    }

    @Override
    public List<String> listConversationsCreatedBetween(DateTime start, DateTime end) {
        // Defer to Riak as the single source of truth; once both are in sync, switch to persistence.strategy = cassandra
        return riakConversationRepository.listConversationsCreatedBetween(start, end);
    }

    @Override
    public Set<String> getConversationsModifiedBefore(DateTime before, int maxResults) {
        // Defer to Riak as the single source of truth; once both are in sync, switch to persistence.strategy = cassandra
        return riakConversationRepository.getConversationsModifiedBefore(before, maxResults);
    }

    @Override
    public Optional<Conversation> findExistingConversationFor(ConversationIndexKey key) {
        Optional<Conversation> conversation = cassandraConversationRepository.findExistingConversationFor(key);

        if (!conversation.isPresent()) {
            conversation = riakConversationRepository.findExistingConversationFor(key);

            if (conversation.isPresent()) {
                migrateEventsToCassandra(conversation.get().getId());
            }
        }

        return conversation;
    }

    @Override
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        List<ConversationEvent> cassandraToBeCommittedEvents = toBeCommittedEvents;

        // Also perform a migration of historic events in case 1) there aren't any yet for this conversation, 2) this
        // isn't already going on

        if (cassandraConversationRepository.getLastModifiedDate(conversationId) == null) {
            List<ConversationEvent> existingEvents = riakConversationRepository.getConversationEvents(conversationId);

            if (existingEvents != null && !existingEvents.isEmpty()) {
                final List<ConversationEvent> combinedEvents = new ArrayList<>(existingEvents);

                toBeCommittedEvents.stream()
                  .filter(event -> !combinedEvents.contains(event))
                  .forEach(event -> combinedEvents.add(event));

                cassandraToBeCommittedEvents = combinedEvents;
            }

            // If migration is already in progress we still have to commit the additional events provided to this call

            if (!migrateEventsToCassandra(conversationId, cassandraToBeCommittedEvents)) {
                cassandraConversationRepository.commit(conversationId, toBeCommittedEvents);
            }
        } else {
            cassandraConversationRepository.commit(conversationId, toBeCommittedEvents);
        }

        riakConversationRepository.commit(conversationId, toBeCommittedEvents);
    }

    @Override
    public void deleteConversation(Conversation c) {
        riakConversationRepository.deleteConversation(c);
        cassandraConversationRepository.deleteConversation(c);
    }

    private boolean migrateEventsToCassandra(String conversationId) {
        return migrateEventsToCassandra(conversationId, null);
    }

    private boolean migrateEventsToCassandra(String conversationId, List<ConversationEvent> events) {
        // Essentially do a cross-cluster synchronize on this particular Conversation id to avoid duplication

        if (!migrationState.tryClaim(Conversation.class, conversationId)) {
            return false;
        }

        LOG.debug("Migrating all events for Conversation with id {} from Riak to Cassandra", conversationId);

        try (Timer.Context ignored = migrationConversationLagTimer.time()) {
            if (events == null) {
                events = riakConversationRepository.getConversationEvents(conversationId);
            }

            cassandraConversationRepository.commit(conversationId, events);
        } finally {
            migrateConversationCounter.inc();
        }

        return true;
    }

    // Defer to Riak as the single source of truth; once both are in sync, switch to persistence.strategy = cassandra
    public long getConversationCount(DateTime start, DateTime end) {
        return riakConversationRepository.getConversationCount(start,end);
    }
}
