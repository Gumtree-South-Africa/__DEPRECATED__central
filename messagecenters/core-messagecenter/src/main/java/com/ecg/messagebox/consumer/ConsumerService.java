package com.ecg.messagebox.consumer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.comaas.events.Conversation;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.replyts.core.runtime.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collections;

import static net.logstash.logback.argument.StructuredArguments.kv;

class ConsumerService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerService.class);

    private static final MetricsService METRICS = MetricsService.getInstance();

    private static final Timer OVERALL_TIMER = METRICS.timer("processing_total");

    private static final Counter FAILED_EVENTS_TOTAL = METRICS.counter("failed_events_total");
    private static final Counter SUCCESSFUL_EVENTS_TOTAL = METRICS.counter("successful_events_total");
    private static final Counter UNKNOWN_EVENT_COUNTER = METRICS.counter("unknown_event_total");

    private static final Counter CONVERSATION_READ_TOTAL_COUNTER = METRICS.counter("conversation_read_total");
    private static final Counter CONVERSATION_READ_FAILED_COUNTER = METRICS.counter("conversation_read_failed");

    private static final Counter CONVERSATION_VISIBILITY_CHANGED_TOTAL_COUNTER = METRICS.counter("conversation_visibility_changed_total");
    private static final Counter CONVERSATION_VISIBILITY_CHANGED_FAILED_COUNTER = METRICS.counter("conversation_visibility_changed_failed");

    private final CassandraPostBoxRepository postBoxRepository;
    private final String tenantShort;

    ConsumerService(CassandraPostBoxRepository postBoxRepository, String tenantShort) {
        this.postBoxRepository = postBoxRepository;
        this.tenantShort = tenantShort;
    }

    void processEvent(Conversation.Envelope envelope) {
        MDC.put("conversation_id", envelope.getConversationId());
        MDC.put("tenant_owner", envelope.getTenant());

        if (!tenantShort.equals(envelope.getTenant())) {
            LOG.debug("ignore the event as belonging to other tenant={}", envelope.getTenant());
            return;
        }

        try (Timer.Context ignored = OVERALL_TIMER.time()) {
            boolean successfullyProcessed;
            if (envelope.hasConversationRead()) {
                CONVERSATION_READ_TOTAL_COUNTER.inc();
                String userId = envelope.getConversationRead().getUserId();
                LOG.debug("Event '{}'", Conversation.ConversationRead.class.getSimpleName(), kv("user_id", userId));
                markConversationAsRead(envelope, userId);
                successfullyProcessed = true;
            } else if (envelope.hasConversationActivated()) {
                CONVERSATION_VISIBILITY_CHANGED_TOTAL_COUNTER.inc();
                Conversation.ConversationActivated conversationActivated = envelope.getConversationActivated();
                String userId = conversationActivated.getUserId();
                LOG.debug("Change visibility event: 'ACTIVATED'", kv("user_id", userId));
                postBoxRepository.activateConversations(userId,
                        postBoxRepository.getConversationAdIdsMap(userId, Collections.singletonList(envelope.getConversationId())));
                successfullyProcessed = true;
            } else if (envelope.hasConversationArchived()) {
                CONVERSATION_VISIBILITY_CHANGED_TOTAL_COUNTER.inc();
                Conversation.ConversationArchived conversationArchived = envelope.getConversationArchived();
                String userId = conversationArchived.getUserId();
                LOG.debug("Change visibility event: 'ACTIVATED'", kv("user_id", userId));
                postBoxRepository.archiveConversations(userId,
                        postBoxRepository.getConversationAdIdsMap(userId, Collections.singletonList(envelope.getConversationId())));
                successfullyProcessed = true;
            } else {
                UNKNOWN_EVENT_COUNTER.inc();
                successfullyProcessed = false;
            }

            if (successfullyProcessed) {
                SUCCESSFUL_EVENTS_TOTAL.inc();
            }
        } catch (Exception ex) {
            LOG.error("Event processing failed.", ex);
            FAILED_EVENTS_TOTAL.inc();

            if (envelope.hasConversationRead()) {
                CONVERSATION_READ_FAILED_COUNTER.inc();
            } else if (envelope.hasConversationArchived() || envelope.hasConversationActivated()) {
                CONVERSATION_VISIBILITY_CHANGED_FAILED_COUNTER.inc();
            }
        } finally {
            MDC.clear();
        }
    }

    private void markConversationAsRead(Conversation.Envelope envelope, String userId) {
        postBoxRepository.getConversationWithMessages(userId, envelope.getConversationId(), null, 1).ifPresent(c ->
                c.getParticipants().stream()
                        .map(Participant::getUserId)
                        .filter(id -> !userId.equals(id))
                        .forEach(otherUserId -> {
                            postBoxRepository.resetConversationUnreadCount(userId, otherUserId, c.getId(), c.getAdId());
                        }));
    }
}
