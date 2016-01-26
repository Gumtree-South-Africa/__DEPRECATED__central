package com.ecg.replyts.app.postprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.core.Ordered;

/**
 * Pluggable Postprocessors. The PostProcessorChain can be extended by custom postprocessors that will modify the
 * outbound E-Mail. One can access the outbound mail via <code>context.getOutgoingMail()</code>.
 * The order of the PostProcessor chain is determined via spring's Ordered comparator.
 */
public interface PostProcessor extends Ordered {

    /**
     * Invoked to modify the outbound e-mail. After Filter chain but before the mail is actually sent out.
     */
    void postProcess(MessageProcessingContext context);

}
