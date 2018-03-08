package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.preprocessorchain.PreProcessorManager;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageFixer;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Represents the mail's flow through various processing stages. Has input methods for all stages (messages can be input
 * everywhere, and they will flow as far as they can get (flow stops at the end, or if it is terminated before)
 */
@Component
class ProcessingFlow {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingFlow.class);

    @Autowired
    private PreProcessorManager preProcessor;

    @Autowired(required = false)
    private List<MessageFixer> javaMailMessageFixers = emptyList();

    @Autowired
    private FilterChain filterChain;

    @Autowired
    private PostProcessorChain postProcessor;

    @Autowired
    private MailDeliveryService mailDeliveryService;

    private final Timer preProcessorTimer = TimingReports.newTimer("preProcessor");
    private final Timer filterChainTimer = TimingReports.newTimer("filterChain");
    private final Timer postProcessorTimer = TimingReports.newTimer("postProcessor");
    private final Timer sendingTimer = TimingReports.newTimer("sending");
    private final Timer mailFixersTimer = TimingReports.newTimer("send-fail-fixers");
    private final Counter emailDeliverySkippedTimer = TimingReports.newCounter("email-delivery-skipped");

    void inputForPreProcessor(MessageProcessingContext context) {
        try (Timer.Context ignore = preProcessorTimer.time()) {
            LOG.trace("PreProcessing Message {}", context.getMessageId());
            preProcessor.preProcess(context);
        }

        if (!context.isTerminated()) {
            inputForFilterChain(context);
        }
    }

    void inputForFilterChain(MessageProcessingContext context) {
        try (Timer.Context ignore = filterChainTimer.time()) {
            LOG.trace("Filtering Message {}", context.getMessageId());
            filterChain.filter(context);
        }

        if (!context.isTerminated()) {
            inputForPostProcessor(context);
        }
    }

    void inputForPostProcessor(MessageProcessingContext context) {
        try (Timer.Context ignore = postProcessorTimer.time()) {
            LOG.trace("PostProcessing Message {}", context.getMessageId());
            postProcessor.postProcess(context);
        }

        if (context.isTerminated()) {
            throw new IllegalStateException("PostProcessors may not Terminate messages");
        }

        inputForSending(context);
    }

    void inputForSending(MessageProcessingContext context) {
        if (context.isSkipDeliveryChannel(MessageProcessingContext.DELIVERY_CHANNEL_MAIL)) {
            emailDeliverySkippedTimer.inc();
            LOG.debug("E-mail delivery switched off for Message {}", context.getMessageId());
            return;
        }

        try {
            doTimedSend(context);
        } catch (MailDeliveryException e) {
            // This can occur when MIME4J was able to parse the incoming email, but JavaMail couldn't parse it on the
            // way out. There's still a chance we could recover by fixing the outgoing email.
            tryMailSendingRecovery(e, context);
        }
    }

    private void tryMailSendingRecovery(MailDeliveryException e, MessageProcessingContext context) {
        LOG.info("Exception on delivery. Trying to recover and retry.");

        try (Timer.Context ignore = mailFixersTimer.time()) {
            context.getOutgoingMail().applyOutgoingMailFixes(javaMailMessageFixers, e);
        }

        try {
            doTimedSend(context);
            LOG.info("Successful recovery.");
        } catch (MailDeliveryException deliveryException) {
            Mail mail = context.getMail().get();
            String msg = String.format("Failed to process mail '%s' from '%s' to '%s'", context.getMessageId(), mail.getFrom(), mail.getDeliveredTo());
            throw new RuntimeException(msg, deliveryException);
        }
    }

    private void doTimedSend(MessageProcessingContext context) throws MailDeliveryException {
        try (Timer.Context ignore = sendingTimer.time()) {
            LOG.trace("Sending Message {}", context.getMessageId());
            mailDeliveryService.deliverMail(context.getOutgoingMail());
        }
    }
}
