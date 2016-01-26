package com.ecg.replyts.core.runtime.persistence.conversation;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * in split brain scenarios (or due to replication drift) some strange things can happen (events that would normally
 * not be allowed to follow after another might do just that after a merge. this class
 */
final class ConversationEventSanityInspector {

    private static final Counter DISPOSED_EVENT_COUNTER = TimingReports.newCounter("disposed-conversation-events-during-merge");

    private static final Logger LOG = LoggerFactory.getLogger(ConversationEventSanityInspector.class);

    private ConversationEvents result;

    private ConversationEventSanityInspector(ConversationEvents events) {
        result = new ConversationEvents(filter(events.getEvents()));
    }

    private List<ConversationEvent> filter(List<ConversationEvent> events) {
        List<ConversationEvent> acceptableEvents = Lists.newArrayList();
        ImmutableConversation replayedConversation = ImmutableConversation.replay(events.subList(0, 1));
        for (ConversationEvent eventToCheck : events) {
            try {
                acceptableEvents.add(eventToCheck);

                replayedConversation = replayedConversation.updateMany(Collections.singletonList(eventToCheck));
            } catch (RuntimeException e) {
                acceptableEvents.remove(eventToCheck);
                DISPOSED_EVENT_COUNTER.inc();
                LOG.warn("Disposing unacceptable event due to ", e);
            }
        }

        return acceptableEvents;
    }

    public ConversationEvents getResult() {
        return result;
    }

    public static ConversationEvents disposeEventsThatLeadToUnresolvableStates(ConversationEvents events) {
        return new ConversationEventSanityInspector(events).getResult();
    }
}
