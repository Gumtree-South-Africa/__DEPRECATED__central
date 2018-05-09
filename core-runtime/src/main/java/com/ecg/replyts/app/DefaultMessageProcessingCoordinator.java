package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
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
import com.google.common.base.Stopwatch;
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
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * Coordinates a message flowing through the system and is therefore the main entry point for a message running through
 * the system.
 */
@Component
public class DefaultMessageProcessingCoordinator implements MessageProcessingCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessageProcessingCoordinator.class);

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("processing-total");
    private static final io.prometheus.client.Counter X_COMAAS_TENANT_COUNTER = io.prometheus.client.Counter.build("ingest_tenant_header_total",
            "Incoming Emails with(out) the X-Comaas-Tenant header").labelNames("present").register();

    private final List<MessageProcessedListener> messageProcessedListeners = new ArrayList<>();
    private final ProcessingFlow processingFlow;
    private final ProcessingFinalizer persister;
    private final ProcessingContextFactory processingContextFactory;
    private final Counter contentLengthCounter;

    @Autowired
    public DefaultMessageProcessingCoordinator(
            @Autowired(required = false) Collection<MessageProcessedListener> messageProcessedListeners,
            ProcessingFlow processingFlow,
            ProcessingFinalizer persister,
            ProcessingContextFactory processingContextFactory) {
        if (messageProcessedListeners != null) {
            this.messageProcessedListeners.addAll(messageProcessedListeners);
        }
        this.processingFlow = checkNotNull(processingFlow, "processingFlow");
        this.persister = checkNotNull(persister, "presister");
        this.processingContextFactory = checkNotNull(processingContextFactory, "processingContextFactory");
        this.contentLengthCounter = TimingReports.newCounter("mail-content-length");
    }

    /**
     * Invoked by a Mail Receiver. The passed input stream is an input stream the the actual mail contents. This method
     * will perform the full message processing and return once the message has reached an end state. If this method
     * throws an exception, an abnormal behaviour occurred during processing, indicating the the Mail Receiver should try
     * to redeliver that message at a later time.
     */
    @Override
    public final Optional<String> accept(@WillNotClose InputStream input) throws IOException, ParsingException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String messageId = Guids.next();
        byte[] bytes = ByteStreams.toByteArray(input);

        try (Timer.Context ignored = OVERALL_TIMER.time()) {
            LOG.debug("Message received", keyValue("contentLength", bytes.length));

            Optional<Mail> mail = parseMail(bytes);
            if (!mail.isPresent()) {
                // Return here, if unparseable, message is persisted in parse mail.
                return Optional.empty();
            }

            boolean containsTenantCode = mail.get().containsHeader("X-Comaas-Tenant");
            X_COMAAS_TENANT_COUNTER.labels(String.valueOf(containsTenantCode)).inc();
            MessageProcessingContext context = processingContextFactory.newContext(mail.get(), messageId);
            setMDC(context);
            contentLengthCounter.inc(bytes.length);
            return Optional.of(handleContext(Optional.of(bytes), context));
        } catch (ParsingException e) {
            LOG.warn("Could not parse mail with id {}", messageId, e);
            handleTermination(Termination.unparseable(e), messageId, Optional.empty(), Optional.empty(), Optional.of(bytes));
            throw e;
        } finally {
            LOG.debug("Message processed", keyValue("processingTime", stopwatch.elapsed(TimeUnit.MILLISECONDS)));
        }
    }

    private Optional<Mail> parseMail(byte[] incomingMailContents) throws ParsingException {
        Mail mail = Mails.readMail(incomingMailContents);
        if (mail.getDeliveredTo() == null) {
            throw new ParsingException("Delivered-To header missing");
        }
        return Optional.of(mail);
    }

    @Override
    public String handleContext(Optional<byte[]> bytes, MessageProcessingContext context) {
        setMDC(context);

        processingFlow.inputForPreProcessor(context);

        if (context.isTerminated()) {
            handleTermination(
                    context.getTermination(),
                    context.getMessageId(),
                    context.getMail(),
                    Optional.ofNullable(((DefaultMutableConversation) context.mutableConversation())),
                    bytes);
        } else {
            handleSuccess(context, bytes);
        }
        return context.getMessageId();
    }

    private void setMDC(MessageProcessingContext context) {
        MDC.put(MESSAGE_ID, context.getMessageId());
        Optional<Mail> mail = context.getMail();
        if (mail.isPresent()) {
            MDC.put(MAIL_ORIGINAL_FROM, mail.get().getFrom());
            MDC.put(MAIL_ORIGINAL_TO, mail.get().getDeliveredTo());
        }
    }

    private void handleSuccess(MessageProcessingContext context, Optional<byte[]> messageBytes) {
        byte[] outgoing = context.getOutgoingMail() != null ? Mails.writeToBuffer(context.getOutgoingMail()) : null;

        persister.persistAndIndex(
                ((DefaultMutableConversation) context.mutableConversation()),
                context.getMessageId(),
                messageBytes,
                Optional.ofNullable(outgoing),
                Termination.sent());

        onMessageProcessed(context.getConversation(), context.getMessage());
    }

    private void handleTermination(Termination termination, String messageId, Optional<Mail> mail,
                                   Optional<DefaultMutableConversation> conversation, Optional<byte[]> messageBytes) {
        checkNotNull(termination);
        checkNotNull(messageId);
        checkNotNull(messageBytes);

        DefaultMutableConversation c = conversation.orElseGet(() ->
                processingContextFactory.deadConversationForMessageIdConversationId(messageId, Guids.next(), mail));

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
