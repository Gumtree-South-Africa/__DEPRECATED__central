package com.ecg.replyts.core.api.persistence;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link Conversation} repository.
 */
public interface ConversationRepository {

    /**
     * Loads a conversation by id.
     *
     * @param conversationId id of the conversation
     * @return the conversation or null when not found
     */
    MutableConversation getById(String conversationId);

    /**
     * Loads a conversation by secret.
     *
     * @param secret the encoded secret of either the buyer or the seller,
     *               without the localized prefix (not null/empty)
     * @return the conversation or null when not found
     */
    MutableConversation getBySecret(String secret);

    /**
     * See if a secret is available for a new conversation.
     *
     * @param secret the encoded secret of either the buyer or the seller,
     *               without the localized prefix (not null/empty)
     * @return true when there is no known conversation that uses the given secret,
     * false when the secret is in use
     */
    boolean isSecretAvailable(String secret);

    /**
     * Lists all conversation id found in repo modified between to given dates.
     */
    List<String> listConversationsModifiedBetween(DateTime start, DateTime end);

    Stream<String> streamConversationsModifiedBetween(DateTime start, DateTime end);

    List<String> listConversationsCreatedBetween(DateTime start, DateTime end);

    Set<String> getConversationsModifiedBefore(DateTime before, int maxResults);

    /**
     * tries to find one single conversation that is between a buyer, a seller and about a specific ad id. (there could be more conversations, but only one is returned
     */
    Optional<Conversation> findExistingConversationFor(ConversationIndexKey key);
}