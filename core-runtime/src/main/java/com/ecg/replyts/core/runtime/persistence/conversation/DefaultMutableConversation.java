package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link com.ecg.replyts.core.api.model.conversation.Conversation} that
 * can be mutated. This class is NOT MULTI-THREAD SAFE.
 * <p/>
 * Either get an instance from
 */
public class DefaultMutableConversation implements MutableConversation {
    private ImmutableConversation conversation;
    private final List<ConversationEvent> toBeCommitted;

    /**
     * Create a new mutable conversation.
     *
     * @param command the initial creation command (not null)
     * @return a mutable conversation
     */
    public static DefaultMutableConversation create(NewConversationCommand command) {
        List<ConversationEvent> events = ImmutableConversation.apply(command);
        ImmutableConversation conversation = ImmutableConversation.replay(events);
        return new DefaultMutableConversation(conversation, events);
    }

    // Only used from conversation repositories.
    DefaultMutableConversation(ImmutableConversation conversation) {
        this(conversation, new ArrayList<>());
    }

    private DefaultMutableConversation(ImmutableConversation conversation, List<ConversationEvent> toBeCommitted) {
        this.conversation = conversation;
        this.toBeCommitted = new ArrayList<>(toBeCommitted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Conversation getImmutableConversation() {
        return conversation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyCommand(ConversationCommand command) {
        List<ConversationEvent> additionalEvents = conversation.apply(command);
        toBeCommitted.addAll(additionalEvents);
        conversation = conversation.updateMany(additionalEvents);
    }

    /**
     * Persist the uncommitted changes.
     */
    public void commit(MutableConversationRepository repository, ConversationEventListeners conversationEventListeners) {
        ImmutableList<ConversationEvent> toBeCommittedEvents = ImmutableList.copyOf(toBeCommitted);
        // There is no point in committing the events when there is a delete event: they'll be deleted anyway.
        if (conversationShouldBeDeleted(toBeCommittedEvents)) {
            repository.deleteConversation(this);
        } else {
            repository.commit(conversation.getId(), toBeCommittedEvents);
        }
        toBeCommitted.clear();
        conversationEventListeners.processEventListeners(conversation, toBeCommittedEvents);
    }

    private boolean conversationShouldBeDeleted(List<ConversationEvent> events) {
        return events.stream().anyMatch(event -> event instanceof ConversationDeletedEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return conversation.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAdId() {
        return conversation.getAdId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuyerId() {
        return conversation.getBuyerId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSellerId() {
        return conversation.getSellerId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserIdFor(ConversationRole role) {
        return conversation.getUserId(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuyerSecret() {
        return conversation.getBuyerSecret();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSellerSecret() {
        return conversation.getSellerSecret();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSecretFor(ConversationRole role) {
        return conversation.getSecretFor(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserId(ConversationRole role) {
        return conversation.getUserId(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime getCreatedAt() {
        return conversation.getCreatedAt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime getLastModifiedAt() {
        return conversation.getLastModifiedAt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversationState getState() {
        return conversation.getState();
    }

    @Override
    public boolean isClosedBy(ConversationRole role) {
        return conversation.isClosedBy(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getCustomValues() {
        return conversation.getCustomValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessages() {
        return conversation.getMessages();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message getMessageById(String messageId) {
        return conversation.getMessageById(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVersion() {
        return conversation.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeleted() {
        return conversation.isDeleted();
    }

    @Override
    public String toString() {
        return conversation.toString();
    }
}
