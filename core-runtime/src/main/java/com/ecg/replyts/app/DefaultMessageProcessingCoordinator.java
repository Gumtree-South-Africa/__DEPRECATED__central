package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.Attachment;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.kafka.MessageEventPublisher;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

    public static final String TENANT_ID_EMAIL_HEADER = "X-Comaas-Tenant";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessageProcessingCoordinator.class);

    private static final String X_MESSAGE_ID_HEADER = "X-Message-ID";
    private static final Timer OVERALL_TIMER = TimingReports.newTimer("processing-total");

    private final List<MessageProcessedListener> messageProcessedListeners = new ArrayList<>();
    private final MessageEventPublisher messageEventPublisher;
    private final ProcessingFlow processingFlow;
    private final ProcessingFinalizer persister;
    private final ProcessingContextFactory processingContextFactory;
    private final Counter contentLengthCounter;

    @Value("${replyts.tenant.short}")
    private String shortTenantName;

    @Autowired
    public DefaultMessageProcessingCoordinator(
            @Autowired(required = false) Collection<MessageProcessedListener> messageProcessedListeners,
            ProcessingFlow processingFlow,
            ProcessingFinalizer persister,
            ProcessingContextFactory processingContextFactory,
            MessageEventPublisher messageEventPublisher) {

        if (messageProcessedListeners != null) {
            this.messageProcessedListeners.addAll(messageProcessedListeners);
        }
        this.messageEventPublisher = messageEventPublisher;
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
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public final boolean accept(String messageIdFromKmail, @WillNotClose InputStream input, MessageTransport transport) throws IOException, ParsingException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] bytes = ByteStreams.toByteArray(input);

        MessageProcessingContext context = null;
        try (Timer.Context ignored = OVERALL_TIMER.time()) {
            LOG.debug("Message received", keyValue("contentLength", bytes.length));

            Optional<Mail> mail = parseMail(bytes);
            if (!mail.isPresent()) {
                // Return here, if unparseable, message is persisted in parse mail.
                return false;
            }

            String messageId = getMessageId(messageIdFromKmail, mail.get());
            context = processingContextFactory.newContext(mail.get(), messageId);
            context.setTransport(transport);
            setMDC(context);
            contentLengthCounter.inc(bytes.length);

            handleContext(Optional.of(bytes), context);
            return true;
        } catch (ParsingException e) {
            LOG.warn("Could not parse mail with id {}", messageIdFromKmail, e);
            handleTermination(context, Termination.unparseable(e), messageIdFromKmail, Optional.empty(), Optional.empty(), Optional.of(bytes),
                    Collections.emptySet());
            throw e;
        } finally {
            LOG.debug("Message processed", keyValue("processingTime", stopwatch.elapsed(TimeUnit.MILLISECONDS)));
        }
    }

    private String getMessageId(String messageIdFromKmail, Mail mail) {
        // This is undocumented behaviour which is set to be removed in COMAAS-1226
        // At the moment we can't ignore X-Message-Id entirely and just write/read the messageId field of the
        // kafka payload, because the emails originating from MP (the tenant itself) rely on this header to
        // render the chat ui correctly. So we can only get rid of the edge case when MP is fully on the post message api.
        // (sorry).
        String messageIdFromHeader = mail.getUniqueHeader(X_MESSAGE_ID_HEADER);
        return messageIdFromHeader != null ? messageIdFromHeader : messageIdFromKmail;
    }

    private Optional<Mail> parseMail(byte[] incomingMailContents) throws ParsingException {
        Mail mail = Mails.readMail(incomingMailContents);
        if (mail.getDeliveredTo() == null) {
            throw new ParsingException("Delivered-To header missing");
        }
        return Optional.of(mail);
    }

    @Override
    public void handleContext(Optional<byte[]> bytes, MessageProcessingContext context) {
        setMDC(context);

        if (context.getOutgoingMail() != null) {
            context.getOutgoingMail().addHeader(TENANT_ID_EMAIL_HEADER, shortTenantName);
        }
        processingFlow.inputForPreProcessor(context);

        if (context.isTerminated()) {
            handleTermination(
                    context,
                    context.getTermination(),
                    context.getMessageId(),
                    context.getMail(),
                    Optional.ofNullable(((DefaultMutableConversation) context.mutableConversation())),
                    bytes,
                    context.getAttachments());
        } else {
            handleSuccess(context, bytes);
        }
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
                Termination.sent(),
                context.getAttachments());

        onMessageProcessed(context, context.getConversation(), context.getMessage());
    }

    private void handleTermination(MessageProcessingContext context, Termination termination, String messageId,
                                   Optional<Mail> mail, Optional<DefaultMutableConversation> conversation,
                                   Optional<byte[]> messageBytes, @Nonnull Collection<Attachment> attachments) {

        checkNotNull(termination);
        checkNotNull(messageId);
        checkNotNull(messageBytes);

        DefaultMutableConversation c = conversation.orElseGet(() ->
                processingContextFactory.deadConversationForMessageIdConversationId(messageId, Guids.next(), mail));

        persister.persistAndIndex(c, messageId, messageBytes, Optional.empty(), termination, attachments);

        onMessageProcessed(context, c, c.getMessageById(messageId));
    }

    private void onMessageProcessed(MessageProcessingContext context, Conversation c, Message m) {
        messageEventPublisher.publish(context, c, m);

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
