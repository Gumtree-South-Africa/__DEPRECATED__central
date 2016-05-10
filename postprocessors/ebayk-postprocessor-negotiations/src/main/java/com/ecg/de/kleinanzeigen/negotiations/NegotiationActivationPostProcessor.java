package com.ecg.de.kleinanzeigen.negotiations;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;

public class NegotiationActivationPostProcessor implements PostProcessor {

    private final Caller caller;

    private static final Counter NEGOTIOATION_FAILED_COUNTER = TimingReports.newCounter("processing_failed_negotiation");

    @Autowired
    public NegotiationActivationPostProcessor(Caller caller) {
        this.caller = caller;
    }


    @Override
    public void postProcess(MessageProcessingContext context) {
        if (containsOffer(context.getMessage())) {
            // post processors only apply if the message is sent. so every message that ends up here is not held, blocked or ignored.
            // if an error occurs DO NOT CATCH IT - let message be reprocessed.
            try {
                caller.execute(Caller.NegotationState.ACTIVE__OFFER_DELIVERED, context.getConversation(), context.getMessage());
            } catch (RuntimeException e) {
                NEGOTIOATION_FAILED_COUNTER.inc();
                throw e;
            }
        }
    }


    boolean containsOffer(Message msg) {
        return msg.getHeaders().containsKey("X-Offerid");
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
