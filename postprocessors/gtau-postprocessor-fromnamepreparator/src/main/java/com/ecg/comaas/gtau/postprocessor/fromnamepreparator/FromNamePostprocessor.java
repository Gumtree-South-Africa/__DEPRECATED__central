package com.ecg.comaas.gtau.postprocessor.fromnamepreparator;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class FromNamePostprocessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FromNamePostprocessor.class);

    private final FromNamePostprocessorConfig config;

    @Autowired
    public FromNamePostprocessor(
            @Value("${replyts.from-name.header.buyer}") String buyerNameHeader,
            @Value("${replyts.from-name.header.seller}") String sellerNameHeader,
            @Value("${replyts.from-name.plugin.order:250}") int order
    ) {
        config = new FromNamePostprocessorConfig(buyerNameHeader, sellerNameHeader, order);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MutableMail outboundMail = context.getOutgoingMail();
        String name = getName(context);
        if (!StringUtils.isEmpty(name)) {
            String mail = String.format("%s <%s>", name, outboundMail.getFrom());
            LOG.trace("Injecting from name: {} into header 'From'", name);
            outboundMail.addHeader("From", mail);
        }
    }

    @Override
    public int getOrder() {
        return config.getOrder();
    }

    private String getName(MessageProcessingContext context) {
        if (context.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
            return context.getConversation().getCustomValues().get(config.getBuyerNameHeader());
        } else {
            return context.getConversation().getCustomValues().get(config.getSellerNameHeader());
        }
    }
}
