package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationModificationDate;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventId;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;

import java.util.stream.Stream;

/**
 * Adds methods to query or delete data specific to Cassandra.
 */
public interface CassandraConversationRepository extends MutableConversationRepository {

    /**
     * Deletes the old conversation modification date.
     * @param conversationModificationDate the conversation id with a modification date to be deleted
     */
    void deleteOldConversationModificationDate(ConversationModificationDate conversationModificationDate);

    /**
     * Streams conversation modification dates for the specified year, month and day.
     * @param year the year
     * @param month the month
     * @param day the day
     * @return the stream with conversations and modification dates
     */
    Stream<ConversationModificationDate> streamConversationModificationsByDay(int year, int month, int day);

    /**
     * Gets the last modified date for a conversation.
     * @param conversationId the conversation id
     * @return the last modified date or null if nothing is found
     */
    DateTime getLastModifiedDate(String conversationId);

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
     * @param date to date to search conversation events
     * @return the stream with conversation event ids
     */
    Stream<ConversationEventId> streamConversationEventIdsByHour(DateTime date);
}