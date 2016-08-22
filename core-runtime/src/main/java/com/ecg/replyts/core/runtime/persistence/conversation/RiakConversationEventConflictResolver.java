package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.cap.ConflictResolver;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.ecg.replyts.core.runtime.persistence.conversation.ConversationEventSanityInspector.disposeEventsThatLeadToUnresolvableStates;

/**
 * Merges concurrently written conversation event lists to a single list.
 */
public class RiakConversationEventConflictResolver implements ConflictResolver<ConversationEvents> {

    private static final Logger LOG = LoggerFactory.getLogger(RiakConversationEventConflictResolver.class);


    private static final Counter MERGE_COUNTER = TimingReports.newCounter("riak-merges");
    private static final Histogram SIBLING_COUNT_HISTOGRAM = TimingReports.newHistogram("riak-sibling-counts");

    @Override
    public ConversationEvents resolve(Collection<ConversationEvents> siblings) {
        if (siblings == null || siblings.isEmpty()) return null;

        if (siblings.size() == 1) return siblings.iterator().next();

        MERGE_COUNTER.inc();
        SIBLING_COUNT_HISTOGRAM.update(siblings.size());
        TimingReports.newCounter("riak-conversation-merges." + siblings.size() + "siblings").inc();
        LOG.info("Merge: {} siblings in conversation '{}'", siblings.size(), getConvId(siblings));

        Iterator<ConversationEvents> iterator = siblings.iterator();
        ConversationEvents result = iterator.next();
        while (iterator.hasNext()) {
            result = mergeNew(result, iterator.next());
        }


        // due to split brain and eventual consistency issues there is a little chance of conversation ending up in
        // unresolvable states when they were merged together.
        // eg: a mail is HELD. Process A sends mail and writes that the mail is SENT to RiakHost1.
        // Process B reads same conversation slightly later from RiakHost2 where the mail still is HELD and decides to
        // BLOCK it. He writes that to RiakHost2. Once the two hosts merge,
        // the mail appears to be first SENT and then BLOCKED, which of course is impossible and causes the conversation
        // to be not loadable. (all replies to this conversation will fail because it can't be loaded
        // This method here will try to filter out such events and remove them in order to keep the conversation working.
        ConversationEvents resolvedEvents = disposeEventsThatLeadToUnresolvableStates(result);
        return new ConversationEvents(resolvedEvents.getEvents(), siblings.size());
    }

    private Object getConvId(Collection<ConversationEvents> siblings) {
        for (ConversationEvents sibling : siblings) {
            for (ConversationEvent conversationEvent : sibling.getEvents()) {
                if (conversationEvent instanceof ConversationCreatedEvent) {
                    return ((ConversationCreatedEvent) conversationEvent).getConversationId();
                }
            }

        }
        return "UNKNOWN CONV";
    }

    private ConversationEvents mergeNew(ConversationEvents left, ConversationEvents right) {
        List<ConversationEvent> mergedEvents = Lists.newArrayList();
        mergedEvents.add(firstConversationCreatedEvent(left, right));
        mergedEvents.addAll(left.followUpEvents());


        List<ConversationEvent> eventsToMergeIn = right.followUpEvents();
        eventsToMergeIn.removeAll(mergedEvents);

        for (ConversationEvent toFitIn : eventsToMergeIn) {
            insertChronologically(toFitIn, mergedEvents);
        }


        return new ConversationEvents(ImmutableList.copyOf(mergedEvents));
    }

    private void insertChronologically(ConversationEvent toFitIn, List<ConversationEvent> mergedEvents) {

        // first event must always be a conversation created event. therefore ensure that no event gets merged in before.
        for (int i = 1; i < mergedEvents.size(); i++) {
            // iterate the list. insert the element to fit in before the first element with a later modification date
            DateTime nextEventsModifiedDate = mergedEvents.get(i).getConversationModifiedAt();
            if (nextEventsModifiedDate.isAfter(toFitIn.getConversationModifiedAt())) {
                mergedEvents.add(i, toFitIn);
                return;
            }
        }
        mergedEvents.add(toFitIn);
    }

    private ConversationCreatedEvent firstConversationCreatedEvent(ConversationEvents left, ConversationEvents right) {
        // if due to datacenter connection loss conversations are recreated, conversations are restored by creating a
        // new conversation created event. When they merge again, the older (first) event has precedence, as we can
        // not use two createdo events
        ConversationCreatedEvent leftCreatedEvent = left.createdEvent();
        ConversationCreatedEvent rightCreatedEvent = right.createdEvent();
        boolean leftEventWasEarlier = leftCreatedEvent.getCreatedAt().isBefore(rightCreatedEvent.getCreatedAt());
        return leftEventWasEarlier ? leftCreatedEvent : rightCreatedEvent;
    }

}
