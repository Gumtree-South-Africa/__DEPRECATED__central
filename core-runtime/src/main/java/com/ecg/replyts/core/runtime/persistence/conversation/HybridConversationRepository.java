package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class HybridConversationRepository implements MutableConversationRepository {
    private static final Counter MIGRATION_CONVERSATION_COUNTER = TimingReports.newCounter("hybrid-conversation-total-migrated");
    private static final Timer MIGRATION_CONVERSATION_TIME_LAG = TimingReports.newTimer("hybrid-conversation-time-migrating");

    private CassandraConversationRepository cassandraConversationRepository;
    private RiakConversationRepository riakConversationRepository;

    public HybridConversationRepository(CassandraConversationRepository cassandraConversationRepository, RiakConversationRepository riakConversationRepository) {
        this.cassandraConversationRepository = cassandraConversationRepository;
        this.riakConversationRepository = riakConversationRepository;
    }

    @Override
    public MutableConversation getById(String conversationId) {
        MutableConversation conversation = null;
        synchronized (cassandraConversationRepository) {
            conversation = cassandraConversationRepository.getById(conversationId);

            if (conversation == null) {
                conversation = riakConversationRepository.getById(conversationId);

                if (conversation != null) {
                    migrateEventsToCassandra(conversationId);
                }
            }
        }
        return conversation;
    }

    @Override
    public MutableConversation getBySecret(String secret) {
        MutableConversation conversation = null;
        synchronized (cassandraConversationRepository) {
            conversation = cassandraConversationRepository.getBySecret(secret);

            if (conversation == null) {
                conversation = riakConversationRepository.getBySecret(secret);

                if (conversation != null) {
                    migrateEventsToCassandra(conversation.getId());
                }
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
        Optional<Conversation> conversation = Optional.absent();
        synchronized (cassandraConversationRepository) {
            conversation = cassandraConversationRepository.findExistingConversationFor(key);

            if (!conversation.isPresent()) {
                conversation = riakConversationRepository.findExistingConversationFor(key);

                if (conversation.isPresent()) {
                    migrateEventsToCassandra(conversation.get().getId());
                }
            }
        }
        return conversation;
    }

    @Override
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {

        List<ConversationEvent> cassandraToBeCommittedEvents = toBeCommittedEvents;
        synchronized (cassandraConversationRepository) {
            if (cassandraConversationRepository.getLastModifiedDate(conversationId) == null) {
                List<ConversationEvent> events = riakConversationRepository.getConversationEvents(conversationId);

                if (events != null && events.size() > 0) {
                    cassandraToBeCommittedEvents = new ArrayList<>(events);

                    cassandraToBeCommittedEvents.addAll(toBeCommittedEvents);
                }
            }

            riakConversationRepository.commit(conversationId, toBeCommittedEvents);

            migrateEventsToCassandra(conversationId, cassandraToBeCommittedEvents);
        }
    }

    @Override
    public void deleteConversation(Conversation c) {
        synchronized (cassandraConversationRepository) {
            riakConversationRepository.deleteConversation(c);
            cassandraConversationRepository.deleteConversation(c);
        }
    }

    private void migrateEventsToCassandra(String conversationId) {
        migrateEventsToCassandra(conversationId, null);
    }

    private void migrateEventsToCassandra(String conversationId, List<ConversationEvent> events) {
        synchronized (cassandraConversationRepository) {
            StopWatch watch = new StopWatch();
            try {
                watch.start();

                if (events == null) {
                    events = riakConversationRepository.getConversationEvents(conversationId);
                }

                cassandraConversationRepository.commit(conversationId, events);

                watch.stop();
            } finally {
                MIGRATION_CONVERSATION_COUNTER.inc();
                MIGRATION_CONVERSATION_TIME_LAG.update(watch.getTotalTimeMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

}
