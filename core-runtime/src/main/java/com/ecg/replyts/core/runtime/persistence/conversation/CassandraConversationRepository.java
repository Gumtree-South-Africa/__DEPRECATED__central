package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIdx;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;

import java.util.stream.Stream;

/**
 * Adds methods to query or delete data specific to Cassandra.
 */
public interface CassandraConversationRepository extends MutableConversationRepository {

    /**
     * Deletes conversation modification indexes of the conversation.
     * @param conversationId the conversation id for which modification indexes should be be deleted
     */
    void deleteConversationModificationIdxs(String conversationId);

    /**
     * Gets the last modified date for a conversation.
     * @param conversationId the conversation id
     * @return the last modified date timestamp or null if nothing is found
     */
    Long getLastModifiedDate(String conversationId);

    /**
     * Low level access to all events in a given date time range.
     * <p/>
     * This implementation loads the first page (by default 5000 elements) and will continue to load the next
     * page as more are requested.
     *
     * @param start lowest date for which you want events
     * @param end   highest date for which you want events
     * @return stream of {@link ImmutablePair}s of Conversation and ConversationEvent
     */
    Stream<ImmutablePair<Conversation, ConversationEvent>> findEventsCreatedBetween(DateTime start, DateTime end);

    /**
     * Streams conversation events created in the hour of the provided date.
     * @param date the date to search conversation events
     * @return the stream with conversation event indexes
     */
    Stream<ConversationEventIdx> streamConversationEventIdxsByHour(DateTime date);

}