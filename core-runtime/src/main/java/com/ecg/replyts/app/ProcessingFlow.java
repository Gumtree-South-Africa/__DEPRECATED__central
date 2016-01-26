package com.ecg.replyts.app;

import com.codahale.metrics.Timer;
import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.preprocessorchain.PreProcessorManager;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * represents the mail's flow through various processing stages. Has input methods for all stages (messages can be input
 * everywhere, and they will flow as far as they can get (flow stops at the end, or if it is terminated before)
 */
class ProcessingFlow {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingFlow.class);

    private final PreProcessorManager preProcessor;

    private final FilterChain filterChain;

    private final PostProcessorChain postProcessor;

    private final MailDeliveryService mailDeliveryService;

    private final Timer preProcessorTimer = TimingReports.newTimer("preProcessor");
    private final Timer filterChainTimer = TimingReports.newTimer("filterChain");
    private final Timer postProcessorTimer = TimingReports.newTimer("postProcessor");
    private final Timer sendingTimer = TimingReports.newTimer("sending");

    @Autowired
    ProcessingFlow(
            MailDeliveryService mailDeliveryService,
            @Qualifier("postProcessorChain") PostProcessorChain postProcessor,
            FilterChain filterChain,
            PreProcessorManager preProcessor
    ) {
        this.mailDeliveryService = mailDeliveryService;
        this.postProcessor = postProcessor;
        this.filterChain = filterChain;
        this.preProcessor = preProcessor;
    }

    public void inputForPreProcessor(MessageProcessingContext context) {

        try (Timer.Context timer = preProcessorTimer.time()) {
            LOG.debug("PreProcessing Message {}", context.getMessageId());
            preProcessor.preProcess(context);
        }
        if (!context.isTerminated()) {
            inputForFilterChain(context);
        }
    }

    public void inputForFilterChain(MessageProcessingContext context) {

        try (Timer.Context timer = filterChainTimer.time()) {
            LOG.debug("Filtering Message {}", context.getMessageId());
            filterChain.filter(context);
        }
        if (!context.isTerminated()) {
            inputForPostProcessor(context);
        }
    }

    public void inputForPostProcessor(MessageProcessingContext context) {

        try (Timer.Context timer = postProcessorTimer.time()) {
            LOG.debug("PostProcessing Message {}", context.getMessageId());
            postProcessor.postProcess(context);
        }
        if (context.isTerminated()) {
            throw new IllegalStateException("PostProcessors may not Terminate messages");
        }
        inputForSending(context);
    }

    public void inputForSending(MessageProcessingContext context) {
        if (context.isSkipDeliveryChannel(MessageProcessingContext.DELIVERY_CHANNEL_MAIL)) {
            return;
        }
        try (Timer.Context timer = sendingTimer.time()) {
            LOG.debug("Sending Message {}", context.getMessageId());
            mailDeliveryService.deliverMail(context.getOutgoingMail());
        } catch (MailDeliveryException e) {
            throw new RuntimeException(e);
        }
    }
}
