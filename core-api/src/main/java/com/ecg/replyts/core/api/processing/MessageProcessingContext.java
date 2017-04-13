package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.google.common.base.Optional;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessageProcessingContext {

    /**
     * The default delivery channel: email
     */
    public static final String DELIVERY_CHANNEL_MAIL = "email";

    private final Mail mail;
    private final String messageId;
    private final MutableMail outgoingMail;

    private Termination termination = null;

    private MessageDirection messageDirection;

    private MutableConversation conversation;

    private final Set<String> skipDeliveryChannels = new HashSet<>();

    private final ProcessingTimeGuard processingTimeGuard;

    private final Map<String, Object> filterContext = new HashMap<>();

    public MessageProcessingContext(Mail mail, String messageId, ProcessingTimeGuard processingTimeGuard) {
        checkNotNull(processingTimeGuard);

        this.mail = mail;
        this.messageId = messageId;
        this.processingTimeGuard = processingTimeGuard;
        outgoingMail = mail.makeMutableCopy();
    }

    public boolean hasConversation() {
        return conversation != null;
    }

    /**
     * @return a reference to the original mail that was received by ReplyTS. This mail is immutable.
     */
    public Mail getMail() {
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
     * @return mail address of the From field - <strong>this does not necessarily need to be the sender</strong> (could also be something like noreply@yourplatform.com for conversation starter mails).
     */
    public MailAddress getOriginalFrom() {
        return new MailAddress(mail.getFrom());
    }

    /**
     * @return mail address of the recipient from the mail's TO field. this either is the real recipient in conversation starter mails, or a cloaked recipient for all follow up mails.
     */
    public MailAddress getOriginalTo() {
        return new MailAddress(mail.getDeliveredTo());
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
     *
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
     * @return the message id of the message the current message is a response to, or null when not found
     */
    public String getInResponseToMessageId() {
        Optional<String> lastReferencesMessageId = mail.getLastReferencedMessageId();
        if (lastReferencesMessageId.isPresent()) {
            String lastRef = lastReferencesMessageId.get();
            List<Message> messages = conversation.getMessages();
            // Iterate in reverse for that odd case that messages have duplicate Message-ID's.
            for (ListIterator<Message> iter = messages.listIterator(messages.size()); iter.hasPrevious(); ) {
                Message message = iter.previous();
                if (lastRef.equals(message.getId())) {
                    return message.getId();
                }
            }
        }
        return null;
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
}
