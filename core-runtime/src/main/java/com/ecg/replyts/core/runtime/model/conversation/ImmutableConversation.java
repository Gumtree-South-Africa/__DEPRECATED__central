package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.api.model.conversation.command.MessageFilteredCommand;
import com.ecg.replyts.core.api.model.conversation.command.MessageModeratedCommand;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationClosedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.CustomValueAddedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageFilteredEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageModeratedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageTerminatedEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Immutable conversation.
 */
public class ImmutableConversation implements Conversation { // NOSONAR

    private static final Logger LOG = LoggerFactory.getLogger(ImmutableConversation.class);

    private final int version;
    private final String id;
    private final String adId;
    private final String buyerId;
    private final String sellerId;
    private final String buyerSecret;
    private final boolean closedByBuyer;
    private final boolean closedBySeller;
    private final String sellerSecret;
    private final DateTime createdAt;
    private final DateTime lastModifiedAt;
    private final ConversationState state;
    private final Map<String, String> customValues;
    private final List<Message> messages;
    private final boolean deleted;

    private ImmutableConversation(Builder builder) {
        Preconditions.checkNotNull(builder.createdAt);
        Preconditions.checkNotNull(builder.lastModifiedAt);
        Preconditions.checkNotNull(builder.state);
        Preconditions.checkNotNull(builder.messages);
        this.version = builder.version;
        this.id = builder.id;
        this.adId = builder.adId;
        this.buyerId = builder.buyerId;
        this.sellerId = builder.sellerId;
        this.buyerSecret = builder.buyerSecret;
        this.sellerSecret = builder.sellerSecret;
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
        this.closedByBuyer = builder.closedByBuyer;
        this.closedBySeller = builder.closedBySeller;
        this.state = builder.state;
        this.customValues = ImmutableMap.copyOf(builder.customValues);
        this.messages = ImmutableList.copyOf(builder.messages);
        this.deleted = builder.isDeleted;
    }


    /**
     * Validate the command against this version of the conversation.
     *
     * @param command the command
     * @return the events that are needed to replay this command
     * @throws IllegalArgumentException when the command is invalid
     */
    public static List<ConversationEvent> apply(NewConversationCommand command) {
        ConversationEvent event = new ConversationCreatedEvent(command);
        return Collections.singletonList(event);
    }

    /**
     * Validate the command against this version of the conversation.
     *
     * @param command the command
     * @return the events that are needed to replay this command
     * @throws IllegalArgumentException when invalid
     * @throws IllegalStateException    when a command is issued after a ConversationDeletedCommand has been processed
     */
    public List<ConversationEvent> apply(ConversationCommand command) {
        if (deleted) {
            throw new IllegalStateException("This conversation has been deleted, further commands should not be issued");
        }
        if (command instanceof AddMessageCommand) {
            return applyInternal((AddMessageCommand) command);
        }
        if (command instanceof MessageFilteredCommand) {
            return applyInternal((MessageFilteredCommand) command);
        }
        if (command instanceof MessageModeratedCommand) {
            return applyInternal((MessageModeratedCommand) command);
        }
        if (command instanceof MessageTerminatedCommand) {
            return applyInternal((MessageTerminatedCommand) command);
        }
        if (command instanceof ConversationClosedCommand) {
            return applyInternal((ConversationClosedCommand) command);
        }
        if (command instanceof AddCustomValueCommand) {
            return applyInternal((AddCustomValueCommand) command);
        }
        if (command instanceof ConversationDeletedCommand) {
            return applyInternal((ConversationDeletedCommand) command);
        }
        throw new IllegalArgumentException("unknown command of type " + command.getClass().getName() + ": " + command);
    }

    private List<ConversationEvent> applyInternal(ConversationDeletedCommand command) {
        return Collections.singletonList(new ConversationDeletedEvent(command));
    }


    /**
     * Validate the command against this version of the conversation.
     *
     * @param command the command
     * @return the events that are needed to replay this command
     * @throws IllegalArgumentException when invalid
     */
    private List<ConversationEvent> applyInternal(AddMessageCommand command) {
        assertMessageIdNotPresent(command.getMessageId());

        ConversationEvent event = new MessageAddedEvent(command);
        return Collections.singletonList(event);
    }

    /**
     * Validate the command against this version of the conversation.
     *
     * @param command the command
     * @return the events that are needed to replay this command
     * @throws IllegalArgumentException when invalid
     */
    private List<ConversationEvent> applyInternal(MessageTerminatedCommand command) {
        assertMessageIdPresent(command.getMessageId());
        // assert that new state is allowed given current state

        ConversationEvent event = new MessageTerminatedEvent(command);
        return Collections.singletonList(event);
    }

    private List<ConversationEvent> applyInternal(AddCustomValueCommand cmd) {
        Preconditions.checkNotNull(cmd.getKey());
        Preconditions.checkNotNull(cmd.getValue());
        return Collections.singletonList(new CustomValueAddedEvent(cmd.getKey(), cmd.getValue()));
    }

    private List<ConversationEvent> applyInternal(ConversationClosedCommand command) {

        ConversationEvent event = new ConversationClosedEvent(command);
        return Collections.singletonList(event);
    }

    /**
     * Validate the command against this version of the conversation.
     *
     * @param command the command
     * @return the events that are needed to replay this command
     * @throws IllegalArgumentException when invalid
     */
    private List<ConversationEvent> applyInternal(MessageFilteredCommand command) {
        assertMessageIdPresent(command.getMessageId());
        // assert that new state is allowed given current state

        ConversationEvent event = new MessageFilteredEvent(command);
        return Collections.singletonList(event);
    }

    /**
     * Validate the command against this version of the conversation.
     *
     * @param command the command
     * @throws IllegalArgumentException when invalid
     */
    private List<ConversationEvent> applyInternal(MessageModeratedCommand command) {
        assertMessageIdPresent(command.getMessageId());
        // assert that new state is allowed given current state

        ConversationEvent event = new MessageModeratedEvent(command);
        return Collections.singletonList(event);
    }

    @Override
    public String getUserId(ConversationRole role) {
        return role == ConversationRole.Buyer ? buyerId : sellerId;
    }

    /**
     * Creates a conversation by replaying a list of {@link ConversationEvent}s.
     *
     * @param events the events, first event must be a {@link ConversationCreatedEvent}
     * @return the new conversation
     */
    public static ImmutableConversation replay(List<ConversationEvent> events) {
        ConversationEvent firstEvent = events.get(0);
        ConversationCreatedEvent createdEvent;

        // Fix the order
        if (!(firstEvent instanceof ConversationCreatedEvent)) {
            LOG.debug("Event order is wrong - attempting to fix - filtering collection of {} ConversationEvents", events.size());
            Predicate<ConversationEvent> isConversatonCreatedEvent = ConversationCreatedEvent.class::isInstance;
            Optional<ConversationEvent> event = events.stream().filter(isConversatonCreatedEvent).findFirst();
            if (!event.isPresent()) {
                throw new InvalidConversationException(events);
            }
            createdEvent =  (ConversationCreatedEvent) event.get();
            // Potentially removes all duplicates of ConversationCreated event - must be a good thing
            events = events.stream().filter(isConversatonCreatedEvent.negate()).collect(Collectors.toCollection(ArrayList::new));
            events.add(0,createdEvent);
        } else {
            createdEvent = (ConversationCreatedEvent) firstEvent;
        }

        ImmutableConversation result = createInternal(createdEvent);
        return result.updateMany(events.subList(1, events.size()));
    }

    public ImmutableConversation updateMany(List<ConversationEvent> conversationEvents) {
        ImmutableConversation result = this;
        for (ConversationEvent conversationEvent : conversationEvents) {
            result = result.update(conversationEvent);
        }
        return result;
    }

    private ImmutableConversation update(ConversationEvent event) {
        if (event instanceof MessageAddedEvent) {
            return updateInternal((MessageAddedEvent) event);
        }
        if (event instanceof MessageFilteredEvent) {
            return updateInternal((MessageFilteredEvent) event);
        }
        if (event instanceof MessageModeratedEvent) {
            return updateInternal((MessageModeratedEvent) event);
        }
        if (event instanceof MessageTerminatedEvent) {
            return updateInternal((MessageTerminatedEvent) event);
        }
        if (event instanceof ConversationClosedEvent) {
            return updateInternal((ConversationClosedEvent) event);
        }
        if (event instanceof CustomValueAddedEvent) {
            return updateInternal((CustomValueAddedEvent) event);
        }
        if (event instanceof ConversationDeletedEvent) {
            return updateInternal((ConversationDeletedEvent) event);
        }
        if (event instanceof ConversationCreatedEvent) {
            // COMAAS-414 No need to call updateInternal
            return this;
        }
        LOG.warn("Ignoring unknown event of type {}: {}", event.getClass().getName(), event);
        return this;
    }

    /**
     * Creates a conversation from the {@link ConversationCreatedEvent event}.
     *
     * @param event an event
     * @return the new conversation
     */
    private static ImmutableConversation createInternal(ConversationCreatedEvent event) {
        return aConversation().
                withId(event.getConversationId()).
                withAdId(event.getAdId()).
                withBuyer(event.getBuyerId(), event.getBuyerSecret()).
                withSeller(event.getSellerId(), event.getSellerSecret()).
                withCreatedAt(event.getCreatedAt()).
                withLastModifiedAt(event.getConversationModifiedAt()).
                withState(event.getState()).
                withCustomValues(event.getCustomValues()).
                build();
    }

    private ImmutableConversation updateInternal(MessageAddedEvent event) {
        return aConversation(this)
                .withMessage(ImmutableMessage.Builder.aMessage()
                        .withState(event.getState())
                        .withId(event.getMessageId())
                        .withMessageDirection(event.getMessageDirection())
                        .withReceivedAt(event.getReceivedAt())
                        .withHeaders(event.getHeaders())
                        .withLastModifiedAt(event.getReceivedAt())
                        .withAttachments(event.getAttachments())
                        .withTextParts(event.getTextParts())
                        .withSenderMessageIdHeader(event.getSenderMessageIdHeader())
                        .withInResponseToMessageId(event.getInResponseToMessageId())
                        .withEventTimeUUID(event.getEventTimeUUID()))
                .withLastModifiedAt(event.getConversationModifiedAt())
                .build();
    }

    private ImmutableConversation updateInternal(CustomValueAddedEvent event) {
        return aConversation(this)
                .addCustomValue(event.getKey(), event.getValue())
                .withLastModifiedAt(event.getConversationModifiedAt())
                .build();
    }

    private ImmutableConversation updateInternal(MessageFilteredEvent event) {
        Message message = getMessageById(event.getMessageId());

        return aConversation(this)
                .updateOrAddMessage(
                        ImmutableMessage.Builder.aMessage(message).
                                withLastModifiedAt(event.getDecidedAt()).
                                withFilterResultState(event.getFilterResultState()).
                                withLastModifiedAt(event.getConversationModifiedAt()).
                                withTextParts(message.getTextParts()).
                                withProcessingFeedback(event.getProcessingFeedback())
                ).withLastModifiedAt(event.getConversationModifiedAt()).
                        build();
    }

    private ImmutableConversation updateInternal(MessageModeratedEvent event) {
        Message message = getMessageById(event.getMessageId());
        ModerationResultState humanResultState = event.getHumanResultState();

        if (message.getState().isFinalEndstate()) {
            // In case of duplication of MessageModeratedEvents just log a duplication and use the first one which has been already applied.
            LOG.debug("State Traversal from State " + message.getState() + " is not allowed. It's an endstate. Message ID: " + message.getId() + ", Required State: " + humanResultState);
            return this;
        } else {
            checkArgument(humanResultState.isAcceptableOutcome(), "Moderation State " + humanResultState + " is not acceptable for moderation Message ID: " + message.getId());
            MessageState newMessageState = humanResultState.allowsSending() ? MessageState.SENT : MessageState.BLOCKED;

            return aConversation(this)
                    .updateOrAddMessage(
                            ImmutableMessage.Builder.aMessage(message).
                                    withLastModifiedAt(event.getDecidedAt()).
                                    withHumanResultState(event.getHumanResultState()).
                                    withLastModifiedAt(event.getConversationModifiedAt()).
                                    withState(newMessageState).
                                    withTextParts(message.getTextParts()).
                                    withLastEditor(event.getEditor())
                    ).withLastModifiedAt(event.getConversationModifiedAt()).build();
        }
    }

    private ImmutableConversation updateInternal(ConversationClosedEvent event) {
        return aConversation(this)
                .withState(ConversationState.CLOSED)
                .closedBy(event.getCloseIssuer())
                .withLastModifiedAt(event.getConversationModifiedAt())
                .build();
    }

    private ImmutableConversation updateInternal(MessageTerminatedEvent event) {
        Message message = getMessageById(event.getMessageId());

        ImmutableMessage.Builder messageToUpdate = ImmutableMessage.Builder.aMessage(message).
                withLastModifiedAt(event.getConversationModifiedAt()).
                withState(event.getTerminationState()).
                withTextParts(message.getTextParts());
        // don't add processing feedback for sent, as this is the default behaviour
        if (event.getTerminationState() != MessageState.SENT) {
            messageToUpdate.withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                    .withFilterName(event.getIssuer())
                    .withFilterInstance("default")
                    .withUiHint(event.getTerminationState().name())
                    .withDescription(event.getReason()));
        }
        return aConversation(this)
                .updateOrAddMessage(
                        messageToUpdate
                ).
                        withLastModifiedAt(event.getConversationModifiedAt()).
                        build();
    }

    private ImmutableConversation updateInternal(ConversationDeletedEvent event) {
        return aConversation(this)
                .delete()
                .withLastModifiedAt(event.getConversationModifiedAt())
                .build();
    }

    private void assertMessageIdNotPresent(String messageId) {
        if (getMessageById(messageId) != null)
            throw new IllegalArgumentException(
                    "Message with id " + messageId +
                            " is already present in conversation " + getId()
            );
    }

    private void assertMessageIdPresent(String messageId) {
        if (getMessageById(messageId) == null)
            throw new IllegalArgumentException("Message with id " + messageId +
                    " is not present in conversation " + getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message getMessageById(String messageId) {
        for (Message message : messages) {
            if (message.getId().equals(messageId)) return message;
        }
        return null;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserIdFor(ConversationRole role) {
        return role == ConversationRole.Buyer ? buyerId : sellerId;
    }

    @Override
    public String getSecretFor(ConversationRole role) {
        return role == ConversationRole.Buyer ? buyerSecret : sellerSecret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAdId() {
        return adId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuyerId() {
        return buyerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSellerId() {
        return sellerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuyerSecret() {
        return buyerSecret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSellerSecret() {
        return sellerSecret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversationState getState() {
        return state;
    }

    @Override
    public boolean isClosedBy(ConversationRole role) {
        return (role == ConversationRole.Seller && closedBySeller) || (role == ConversationRole.Buyer && closedByBuyer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getCustomValues() {
        return customValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return id;
    }

    public static final class Builder {

        // private static final transient Logger LOG = LoggerFactory.getLogger(Builder.class);
        private int version = 0;
        private String id;
        private String adId;
        private String buyerId;
        private String sellerId;
        private String buyerSecret;
        private String sellerSecret;
        private boolean closedByBuyer;
        private boolean closedBySeller;
        private DateTime createdAt = new DateTime();
        private DateTime lastModifiedAt;
        private ConversationState state;
        private Map<String, String> customValues = new HashMap<>();
        private List<Message> messages = new ArrayList<>();
        private boolean isDeleted;

        private Builder() {
        }

        public static Builder aConversation(Conversation conversation) {
            Builder builder = new Builder().
                    withId(conversation.getId()).
                    withAdId(conversation.getAdId()).
                    withBuyer(conversation.getBuyerId(), conversation.getBuyerSecret()).
                    withSeller(conversation.getSellerId(), conversation.getSellerSecret()).
                    withCreatedAt(conversation.getCreatedAt()).
                    withLastModifiedAt(conversation.getLastModifiedAt()).
                    withState(conversation.getState()).
                    withCustomValues(new HashMap<>(conversation.getCustomValues())).
                    withMessages(new ArrayList<>(conversation.getMessages()));
            builder.version = conversation.getVersion() + 1;
            builder.closedBySeller = conversation.isClosedBy(ConversationRole.Seller);
            builder.closedByBuyer = conversation.isClosedBy(ConversationRole.Buyer);
            if (conversation.isDeleted()) builder.delete();
            return builder;
        }

        public static Builder aConversation() {
            return new Builder();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder closedBy(ConversationRole role) {
            if (role == ConversationRole.Buyer) {
                closedByBuyer = true;
            } else if (role == ConversationRole.Seller) {
                closedBySeller = true;
            }
            return this;
        }

        public Builder withAdId(String adId) {
            this.adId = adId;
            return this;
        }

        public Builder withBuyer(String buyerId, String buyerSecret) {
            this.buyerId = buyerId;
            this.buyerSecret = buyerSecret;
            return this;
        }

        public Builder withSeller(String sellerId, String sellerSecret) {
            this.sellerId = sellerId;
            this.sellerSecret = sellerSecret;
            return this;
        }

        public Builder withCreatedAt(DateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withLastModifiedAt(DateTime lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public Builder withState(ConversationState state) {
            this.state = state;
            return this;
        }

        public Builder withCustomValues(Map<String, String> customValues) {
            this.customValues = customValues;
            return this;
        }

        public Builder addCustomValue(String key, String value) {
            customValues.put(key, value);
            return this;
        }

        public Builder withMessages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder withMessage(ImmutableMessage.Builder builder) {
            this.messages.add(builder.build());
            return this;
        }

        public Builder updateOrAddMessage(ImmutableMessage.Builder builder) {
            Message updatedMessage = builder.build();
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                if (message.getId().equals(updatedMessage.getId())) {
                    messages.set(i, updatedMessage);
                    return this;
                }
            }
            // Not found, add at end
            messages.add(updatedMessage);
            return this;
        }

        public Builder delete() {
            isDeleted = true;
            return this;
        }

        public ImmutableConversation build() {
            if (lastModifiedAt == null) {
                lastModifiedAt = createdAt;
            }
            return new ImmutableConversation(this);
        }
    }
}
