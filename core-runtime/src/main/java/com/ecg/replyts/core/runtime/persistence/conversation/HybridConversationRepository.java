package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.replay;

public class HybridConversationRepository implements MutableConversationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(HybridConversationRepository.class);

    @Value("${persistence.cassandra.commit.max.batch.size:20}")
    private int maxBatchSizeCassandra = 20;

    private final Counter migrateConversationCounter = TimingReports.newCounter("migration.migrate-conversation");
    private final Timer migrationConversationLagTimer = TimingReports.newTimer("migration.migrate-conversation-lag");

    private final DefaultCassandraConversationRepository cassandraConversationRepository;
    private final RiakConversationRepository riakConversationRepository;
    private final HybridMigrationClusterState migrationState;
    private final boolean deepMigrationEnabled;

    public HybridConversationRepository(DefaultCassandraConversationRepository cassandraConversationRepository, RiakConversationRepository riakConversationRepository, HybridMigrationClusterState migrationState, boolean deepMigrationEnabled) {
        this.cassandraConversationRepository = cassandraConversationRepository;
        this.riakConversationRepository = riakConversationRepository;
        this.migrationState = migrationState;
        this.deepMigrationEnabled = deepMigrationEnabled;
    }

    public MutableConversation getByIdWithDeepComparison(String conversationId) {
        LOG.debug("Deep comparing Conversation {} in Riak and Cassandra", conversationId);

        List<ConversationEvent> conversationEventsInRiak = riakConversationRepository.getConversationEvents(conversationId);
        List<ConversationEvent> conversationEventsInCassandra = cassandraConversationRepository.getConversationEvents(conversationId);

        if (conversationEventsInRiak == null) {
            LOG.warn("No conversationEvents found in Riak for conversationId {}, skipping", conversationId);
            return null;
        }

        if (conversationEventsInRiak.size() == conversationEventsInCassandra.size()) {
            LOG.debug("Conversation {} has equal number of conversationEvents ({}) in Riak and Cassandra, not migrating", conversationId, conversationEventsInRiak.size());
            return new DefaultMutableConversation(replay(conversationEventsInRiak));
        }

        // Essentially do a cross-cluster synchronize on this particular Conversation id to avoid duplication
        if (!migrationState.tryClaim(Conversation.class, conversationId)) {
            LOG.warn("Could not claim lock on Conversation {}, not migrating it", conversationId);
            return new DefaultMutableConversation(replay(conversationEventsInRiak));
        }

        // Get the events that are in Riak but not in Cassandra
        List<ConversationEvent> originalConversationEventsInRiak = new ArrayList<>(conversationEventsInRiak);
        for (ConversationEvent eventInCassandra : conversationEventsInCassandra) {
            if (!conversationEventsInRiak.remove(eventInCassandra)) {
                LOG.error("Conversation {} contains a conversationEvent in Cassandra but not in Riak: {}", conversationId, eventInCassandra);
            }
        }

        // save the conversationEvents from Riak to Cassandra
        if (conversationEventsInRiak.size() > 0) {
            commitToCassandraInBatches(conversationId, conversationEventsInRiak);
        }

        // return the Riak state
        return new DefaultMutableConversation(replay(originalConversationEventsInRiak));
    }

    private void commitToCassandraInBatches(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        if (toBeCommittedEvents == null || toBeCommittedEvents.size() == 0) {
            return;
        }
        LOG.info("Committing conversationId {} with {} events in batches of {}", conversationId, toBeCommittedEvents.size(), maxBatchSizeCassandra);

        while (toBeCommittedEvents.size() > maxBatchSizeCassandra) {
            List<ConversationEvent> subList = toBeCommittedEvents.subList(0, maxBatchSizeCassandra);

            if (LOG.isDebugEnabled()) {
                subList.forEach(event -> {
                    if (event instanceof MessageAddedEvent) {
                        int sum = ((MessageAddedEvent) event).getTextParts().stream().mapToInt(textPart -> textPart.getBytes().length).sum();
                        LOG.debug("Committing event. id: {} type: {} textPart size: {}", event.getEventId(), event.getClass().getName(), humanReadableByteCount(sum));
                    } else {
                        LOG.debug("Committing event. id: {} type: {}", event.getEventId(), event.getClass().getName());
                    }
                });
            }

            cassandraConversationRepository.commit(conversationId, subList);

            subList.clear();
        }
        cassandraConversationRepository.commit(conversationId, toBeCommittedEvents);
    }

    @Override
    public MutableConversation getById(String conversationId) {
        MutableConversation conversationInCassandra = cassandraConversationRepository.getById(conversationId);

        if (conversationInCassandra != null) {
            if (deepMigrationEnabled) {
                return getByIdWithDeepComparison(conversationId);
            }
            return conversationInCassandra;
        }

        MutableConversation conversationInRiak = riakConversationRepository.getById(conversationId);
        if (conversationInRiak == null) {
            return null;
        }

        migrateEventsToCassandra(conversationId);
        return conversationInRiak;
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
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {

        // save to riak
        try {
            riakConversationRepository.commit(conversationId, toBeCommittedEvents);
        } catch (Exception e) {

            LOG.error("Failed to save {} conversationEvents for conversation {} to riak, reason {}", toBeCommittedEvents.size(), conversationId, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        List<ConversationEvent> cassandraToBeCommittedEvents = toBeCommittedEvents;

        // Also perform a migration of historic events in case 1) there aren't any yet for this conversation, 2) this
        // isn't already going on
        if (cassandraConversationRepository.getLastModifiedDate(conversationId) == null) {
            // conversation does not exist in cassandra

            List<ConversationEvent> existingEvents = riakConversationRepository.getConversationEvents(conversationId);

            if (existingEvents != null && !existingEvents.isEmpty()) {
                // conversation exists in riak, combine riak events with new events and save to cassandra
                final List<ConversationEvent> combinedEvents = new ArrayList<>(existingEvents);

                toBeCommittedEvents.stream()
                        .filter(event -> !combinedEvents.contains(event))
                        .forEach(event -> combinedEvents.add(event));

                cassandraToBeCommittedEvents = combinedEvents;
            }

            // conversation may or may not exist in riak

            // If migration is already in progress we still have to commit the additional events provided to this call
            if (!migrateEventsToCassandra(conversationId, cassandraToBeCommittedEvents)) {
                cassandraConversationRepository.commit(conversationId, toBeCommittedEvents);
            }
        } else {
            // conversation already in cassandra, just save the new events
            cassandraConversationRepository.commit(conversationId, toBeCommittedEvents);
        }

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
        LOG.debug("Migrating events for Conversation with id {} from Riak to Cassandra", conversationId);

        // Essentially do a cross-cluster synchronize on this particular Conversation id to avoid duplication
        if (!migrationState.tryClaim(Conversation.class, conversationId)) {
            LOG.warn("Could not claim lock on Conversation {}, not migrating it", conversationId);
            return false;
        }

        try (Timer.Context ignored = migrationConversationLagTimer.time()) {
            if (events == null) {
                events = riakConversationRepository.getConversationEvents(conversationId);
            }
            LOG.debug("Migrating Conversation {} with {} conversationEvents from Riak to Cassandra", conversationId, events.size());
            events = fixEventOrder(events);
            cassandraConversationRepository.commit(conversationId, events);
        } finally {
            migrateConversationCounter.inc();
        }

        return true;
    }

    // Make sure that timeUUID of each conversation matches the order of the conversation event in the list
    List<ConversationEvent> fixEventOrder(List<ConversationEvent> cevents) {
        // Assume the order of things is correct, but UUID might not be
        List<ConversationEvent> orderedEvents = new ArrayList<>(cevents);
        ConversationEvent firstEvent = orderedEvents.get(0);

        // Make sure ConversationCreatedEvent is always first
        if (!(firstEvent instanceof ConversationCreatedEvent)) {

            Optional<ConversationEvent> cce = orderedEvents.parallelStream().filter(e -> e instanceof ConversationCreatedEvent).findFirst();
            if (cce.isPresent()) {
                ConversationEvent ce = cce.get();
                orderedEvents.remove(ce);
                orderedEvents.add(0, ce);
                LOG.debug("Moving ConversationCreatedEvent to be the first on the list for event {}", ce.getEventId());
            }
        }
        UUID prevUUID = null;
        for (ConversationEvent e : orderedEvents) {

            if (prevUUID == null) {
                prevUUID = e.getEventTimeUUID();
                continue;
            }
            if (prevUUID.timestamp() >= e.getEventTimeUUID().timestamp()) {
                LOG.debug("Correcting event order for event {}", e.getEventId());
                e.setEventTimeUUID(UUIDs.timeBased());
            }
            prevUUID = e.getEventTimeUUID();
        }
        return orderedEvents;
    }

    private String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
