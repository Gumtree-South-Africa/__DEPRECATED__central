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
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.WillNotClose;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.MAIL_ORIGINAL_FROM;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.MAIL_ORIGINAL_TO;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.MESSAGE_ID;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Coordinates a message flowing through the system and is therefore the main entry point for a message running through
 * the system.
 */
@Component
public class DefaultMessageProcessingCoordinator implements MessageProcessingCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessageProcessingCoordinator.class);

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("processing-total");

    private final Guids guids;
    private final List<MessageProcessedListener> messageProcessedListeners = new ArrayList<>();
    private final ProcessingFlow processingFlow;
    private final ProcessingFinalizer persister;
    private final ProcessingContextFactory processingContextFactory;

    @Autowired
    public DefaultMessageProcessingCoordinator(Guids guids,
                                               @Autowired(required = false) Collection<MessageProcessedListener> messageProcessedListeners,
                                               ProcessingFlow processingFlow, ProcessingFinalizer persister,
                                               ProcessingContextFactory processingContextFactory) {
        this.guids = checkNotNull(guids, "guids");
        if (messageProcessedListeners != null) {
            this.messageProcessedListeners.addAll(messageProcessedListeners);
        }
        this.processingFlow = checkNotNull(processingFlow, "processingFlow");
        this.persister = checkNotNull(persister, "presister");
        this.processingContextFactory = checkNotNull(processingContextFactory, "processingContextFactory");
    }

    /**
     * Invoked by a Mail Receiver. The passed input stream is an input stream the the actual mail contents. This method
     * will perform the full message processing and return once the message has reached an end state. If this method
     * throws an exception, an abnormal behaviour occurred during processing, indicating the the Mail Receiver should try
     * to redeliver that message at a later time.
     */
    @Override
    public final Optional<String> accept(@WillNotClose InputStream input) throws IOException, ParsingException {
        try (Timer.Context ignored = OVERALL_TIMER.time()) {
            byte[] bytes = ByteStreams.toByteArray(input);
            LOG.trace("Received new message. Size {} bytes", bytes.length);

            Optional<Mail> mail = parseMail(bytes);

            if (!mail.isPresent()) {
                // Return here, if unparseable, message is persisted in parse mail.
                return Optional.empty();
            }

            MessageProcessingContext context = processingContextFactory.newContext(mail.get(), guids.nextGuid());
            setMDC(context);
            LOG.debug("Received Message");

            processingFlow.inputForPreProcessor(context);

            if (context.isTerminated()) {
                handleTermination(
                        context.getTermination(),
                        context.getMessageId(),
                        Optional.ofNullable(context.getMail()),
                        Optional.ofNullable(((DefaultMutableConversation) context.mutableConversation())),
                        bytes);
            } else {
                handleSuccess(context, bytes);
            }
            return Optional.of(context.getMessageId());
        }
    }

    private void setMDC(MessageProcessingContext context) {
        MDC.put(MESSAGE_ID, context.getMessageId());
        MDC.put(MAIL_ORIGINAL_FROM, context.getOriginalFrom().getAddress());
        MDC.put(MAIL_ORIGINAL_TO, context.getOriginalTo().getAddress());
    }

    private Optional<Mail> parseMail(byte[] incomingMailContents) throws ParsingException {
        try {
            Mail mail = Mails.readMail(incomingMailContents);

            if (mail.getDeliveredTo() == null) {
                throw new ParsingException("Delivered-To header missing");
            }

            return Optional.of(mail);
        } catch (ParsingException parsingException) {
            final String messageId = guids.nextGuid();
            LOG.warn("Could not parse mail with id {}", messageId, parsingException);
            handleTermination(Termination.unparseable(parsingException), messageId, Optional.empty(), Optional.empty(), incomingMailContents);
            throw parsingException;
        }
    }

    private void handleSuccess(MessageProcessingContext context, byte[] messageBytes) {
        LOG.debug("Message successfully sent.");

        byte[] outgoing = Mails.writeToBuffer(context.getOutgoingMail());

        persister.persistAndIndex(
                ((DefaultMutableConversation) context.mutableConversation()),
                context.getMessageId(),
                messageBytes,
                Optional.of(outgoing),
                Termination.sent());

        onMessageProcessed(context.getConversation(), context.getMessage());
    }

    private void handleTermination(Termination termination, String messageId, Optional<Mail> mail,
                                   Optional<DefaultMutableConversation> conversation, byte[] messageBytes) {
        checkNotNull(termination);
        checkNotNull(messageId);
        checkNotNull(messageBytes);

        DefaultMutableConversation c = conversation.orElseGet(() ->
                processingContextFactory.deadConversationForMessageIdConversationId(messageId, guids.nextGuid(), mail));

        persister.persistAndIndex(c, messageId, messageBytes, Optional.empty(), termination);

        onMessageProcessed(c, c.getMessageById(messageId));
    }

    private void onMessageProcessed(Conversation c, Message m) {
        for (MessageProcessedListener listener : messageProcessedListeners) {
            LOG.trace("Informing Message Processed listener {} about completed message", listener.getClass());

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
