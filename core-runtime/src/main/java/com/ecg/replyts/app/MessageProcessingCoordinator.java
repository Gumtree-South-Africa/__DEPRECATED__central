package com.ecg.replyts.app;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Coordinates a Messages flow through ReplyTS and therefore is the main entry
 * point for a message running through the system.
 */
public class MessageProcessingCoordinator {

    private final Mails mails;

    private final ProcessingFinalizer persister;

    private final Guids guids;

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("processing-total");

    private final ProcessingFlow processingFlow;

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessingCoordinator.class);


    private final List<MessageProcessedListener> messageProcessedListeners;

    private final ProcessingContextFactory processingContextFactory;


    @Autowired
    public MessageProcessingCoordinator(Guids guids, ProcessingFinalizer persister, ProcessingFlow processingFlow,
                                        List<MessageProcessedListener> messageProcessedListeners, ProcessingContextFactory processingContextFactory) {
        this(guids, persister, new Mails(), processingFlow, messageProcessedListeners, processingContextFactory);
    }

    MessageProcessingCoordinator(Guids guids, ProcessingFinalizer persister, Mails mails,
                                 ProcessingFlow processingFlow, List<MessageProcessedListener> messageProcessedListeners,
                                 ProcessingContextFactory processingContextFactory) {
        this.guids = guids;
        this.persister = persister;
        this.mails = mails;
        this.processingFlow = processingFlow;
        this.messageProcessedListeners = messageProcessedListeners;
        this.processingContextFactory = processingContextFactory;

    }

    /**
     * Invoked by a Mail Receiver. The passed input stream is an input stream
     * the the actual mail contents. This method will perform the full message
     * processing and return once the message has reached an end state. If this
     * method throws an exception, an abnormal behaviour occured during
     * processing, indicating the the Mail Receiver should try to redeliver that
     * message at a later time.
     */
    public void accept(InputStream input) throws IOException {

        try (Timer.Context ignored = OVERALL_TIMER.time()) {
            byte[] bytes = ByteStreams.toByteArray(input);
            LOG.debug("received new message. Size {} bytes", bytes.length);
            Optional<Mail> mail = parseMail(bytes);
            if (!mail.isPresent()) {
                // return here, if unparsable, message is persisted in parse mail. 
                return;
            }
            MessageProcessingContext context = processingContextFactory.newContext(mail.get(), guids.nextGuid());
            LOG.debug("Received Message {}", context.getMessageId());
            processingFlow.inputForPreProcessor(context);
            if (context.isTerminated()) {
                handleTermination(
                        context.getTermination(),
                        context.getMessageId(),
                        fromNullable(context.getMail()),
                        fromNullable(((DefaultMutableConversation) context.mutableConversation())),
                        bytes);
            } else {
                handleSuccess(context, bytes);
            }
        }
    }

    private Optional<Mail> parseMail(byte[] incomingMailContents) {
        try {
            Optional<Mail> mail = Optional.of(mails.readMail(incomingMailContents));
            if (mail.get().getDeliveredTo() == null) {
                throw new ParsingException("Delivered-To header missing");
            }
            return mail;

        } catch (ParsingException e) {
            Termination termination = Termination.unparseable(e);
            String messageId = guids.nextGuid();
            handleTermination(termination, messageId, Optional.absent(), Optional.absent(), incomingMailContents);
        }

        return Optional.absent();
    }


    private void handleSuccess(MessageProcessingContext context, byte[] messageBytes) {
        LOG.debug("Message {} (conversation: {})successfully sent.",
                context.getMessageId(),
                context.getConversation().getId());

        byte[] outgoing = mails.writeToBuffer(context.getOutgoingMail());
        persister.persistAndIndex(
                ((DefaultMutableConversation) context.mutableConversation()),
                context.getMessageId(),
                messageBytes,
                Optional.of(outgoing),
                Termination.sent());

        handleProcessedMessageListener(context.getConversation(), context.getMessage());
    }

    private void handleTermination(Termination termination, String messageId, Optional<Mail> mail,
                                   Optional<DefaultMutableConversation> conversation, byte[] messageBytes) {
        checkNotNull(termination);
        checkNotNull(messageId);
        checkNotNull(messageBytes);

        DefaultMutableConversation c = conversation.isPresent() ?
                conversation.get() :
                processingContextFactory.deadConversationForMessageIdConversationId(messageId, guids.nextGuid(), mail);

        persister.persistAndIndex(c, messageId, messageBytes, Optional.absent(), termination);

        handleProcessedMessageListener(c, c.getMessageById(messageId));

    }

    private void handleProcessedMessageListener(Conversation c, Message m) {

        for (MessageProcessedListener listener : messageProcessedListeners) {
            LOG.debug("Informing Message Processed listener {} about completed message", listener.getClass());
            try {
                listener.messageProcessed(c, m);
            } catch (Throwable e) {
                // We want to isolate failures in listeners.
                // They're implemented in plugins and may be packaged/deployed separately, so
                // they may also fail independently from others.
                // This will also catch non-recoverable errors like out-of-memory exceptions.
                // The reasoning is that it's better to try to catch all known exceptions and
                // attempt to continue than cause the thread to crash and possibly lose data
                // by not executing other listeners.
                // Many Errors like LinkageErrors are not recoverable only for a specific listener
                // (others wouldn't be affected).
                LOG.error("Error in MessageProcessedListener " + listener.getClass(), e);
            }
        }
    }

}
