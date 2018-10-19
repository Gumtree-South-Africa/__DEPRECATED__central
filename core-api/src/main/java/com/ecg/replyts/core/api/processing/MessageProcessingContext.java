package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessageProcessingContext {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessingContext.class);

    /**
     * The default delivery channel: email
     */
    public static final String DELIVERY_CHANNEL_MAIL = "email";

    private final Optional<Mail> mail;
    private final String messageId;
    private final MutableMail outgoingMail;
    private final BiPredicate<MailAddress, MailAddress> overrideRecipientPredicate;
    private final ProcessingTimeGuard processingTimeGuard;

    private final Set<String> skipDeliveryChannels = new HashSet<>();
    private final Map<String, Object> filterContext = new HashMap<>();
    private final Collection<Attachment> attachments;

    private Termination termination = null;
    private MessageDirection messageDirection;
    private MutableConversation conversation;
    private MessageTransport transport;
    private String owner;

    @VisibleForTesting
    public MessageProcessingContext(Mail mail, String messageId, ProcessingTimeGuard processingTimeGuard) {
        this(mail, messageId, processingTimeGuard, (toRecipient, storeRecipient)  -> false, Collections.emptySet());
    }

    public MessageProcessingContext(Mail incomingMail, String messageId, ProcessingTimeGuard processingTimeGuard,
                                    BiPredicate<MailAddress, MailAddress> overrideRecipientPredicate,
                                    @Nonnull Collection<Attachment> attachments) {
        checkNotNull(processingTimeGuard);
        this.mail = Optional.ofNullable(incomingMail);
        this.outgoingMail = mail.map(Mail::makeMutableCopy).orElse(null);
        this.messageId = messageId;
        this.processingTimeGuard = processingTimeGuard;
        this.overrideRecipientPredicate = overrideRecipientPredicate;
        this.attachments = attachments;
    }

    public boolean hasConversation() {
        return conversation != null;
    }

    /**
     * @return a reference to the original mail that was received by ReplyTS. This mail is immutable.
     */
    public Optional<Mail> getMail() {
        return mail;
    }

    /**
     * @return a reference to the outbound mail (the one that is sent out to the final recipient). mail is mutable and can be modified therefore.
     */
    public MutableMail getOutgoingMail() {
        return outgoingMail;
    }

    /**
     * executes a modification command to the existing conversation (e.g. add a message, create a conversation,...)
     */
    public void addCommand(ConversationCommand command) {
        conversation.applyCommand(command);
    }


    public void setMessageDirection(MessageDirection direction) {
        this.messageDirection = direction;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    /**
     * @return real sender of the current email processing, a value is not available sooner than the conversation is created or fetched.
     */
    public MailAddress getSender() {
        return getMailStoredMember(MessageDirection::getFromRole);
    }

    /**
     * @return real sender of the current email processing, a value is not available sooner than the conversation is created or fetched.
     */
    public MailAddress getRecipient() {
        MailAddress storedRecipient = getMailStoredMember(MessageDirection::getToRole);

        if (mail.isPresent()) {
            String toRecipientAddress = mail.get().getDeliveredTo();

            if (StringUtils.isNotBlank(toRecipientAddress)) {
                MailAddress toRecipient = new MailAddress(toRecipientAddress);

                // Current stored e-mail in conversation events is overridden by a new email from SMTP TO header
                if (overrideRecipientPredicate.test(toRecipient, storedRecipient)) {
                    LOG.debug("Recipient of Outgoing mail overridden: {}", toRecipient);
                    return toRecipient;
                }
            }
        }

        return storedRecipient;
    }

    private MailAddress getMailStoredMember(Function<MessageDirection, ConversationRole> roleExtractor) {
        if (conversation != null && messageDirection != null) {
            ConversationRole fromRole = roleExtractor.apply(messageDirection);
            return new MailAddress(conversation.getUserId(fromRole));
        }

        throw new IllegalStateException("Cannot retrieve information about real email's members when conversation or message_direction are not known.");
    }

    /**
     * @return true, if message is already terminated (meaning that the mail will not be sent and the message processing is about to halt)
     */
    public boolean isTerminated() {
        return termination != null;
    }

    /**
     * marks this message as "terminated". There is no need to further process this message because it has reached an end state and only needs to be persisted to database.
     *
     * @param state  final state the message should end up in
     * @param issuer reference to the class that terminated the processing (will be logged with the message)
     * @param reason human readable reason why this message ended up in that state.
     */
    public void terminateProcessing(MessageState state, Object issuer, String reason) {
        this.termination = new Termination(state, issuer.getClass(), reason);
    }

    /**
     * @return termination info about that mail, if the mail {@link #isTerminated()}. If not, returns <code>null</code>
     */
    public Termination getTermination() {
        return termination;
    }


    public void setConversation(MutableConversation conversation) {
        this.conversation = conversation;
    }

    /**
     * @return this mail's conversation with all associated messages in it. The returned conversation is immutable. to obtain a mutable conversation, invoke {@link #mutableConversation()}
     */
    public Conversation getConversation() {
        return conversation.getImmutableConversation();
    }

    /**
     * @return this mail's conversation that can be modified by adding commands to it.
     */
    public MutableConversation mutableConversation() {
        return conversation;
    }

    /**
     * @return the message that is about to be processed.
     */
    public Message getMessage() {
        return conversation.getMessageById(messageId);
    }

    /**
     * @return the persisted id of that message.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * adds a delivery channel to the list of channels to ignore for this message.
     * <p>
     * The default delivery channel is {@link #DELIVERY_CHANNEL_MAIL}
     *
     * @param deliveryChannel the delivery channel to ignore.
     */
    public void skipDeliveryChannel(String deliveryChannel) {
        this.skipDeliveryChannels.add(deliveryChannel);
    }

    /**
     * whether the given delivery channel should be skipped.
     *
     * @param deliveryChannel The delivery channel to query
     * @return whether delivery through this channel should be skipped
     */
    public boolean isSkipDeliveryChannel(String deliveryChannel) {
        return this.skipDeliveryChannels.contains(deliveryChannel);
    }

    /**
     * @return The processing time guard. A filter should check if allowed to go on with processing.
     */
    public ProcessingTimeGuard getProcessingTimeGuard() {
        return processingTimeGuard;
    }

    public Map<String, Object> getFilterContext() {
        return filterContext;
    }

    @Nonnull
    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("messageId", messageId)
                .add("conversationId", conversation.getId())
                .toString();
    }

    public void setTransport(MessageTransport transport) {
        this.transport = transport;
    }

    public MessageTransport getTransport() {
        return transport;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
