package com.ebay.ecg.australia.replyts.fromname;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author mdarapour
 */
public class FromNamePostprocessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FromNamePostprocessor.class);

    private FromNamePostprocessorConfig config;

    @Autowired
    public FromNamePostprocessor(@Value("${replyts.from-name.header.buyer}") String buyerNameHeader,
                                 @Value("${replyts.from-name.header.seller}") String sellerNameHeader,
                                 @Value("${replyts.from-name.plugin.order:250}") int order) {
        config = new FromNamePostprocessorConfig(buyerNameHeader, sellerNameHeader, order);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        try {
            MutableMail outboundMail = context.getOutgoingMail();
            String name = null;
            if (context.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
                name = context.getConversation().getCustomValues().get(config.getBuyerNameHeader());
            } else {
                name = context.getConversation().getCustomValues().get(config.getSellerNameHeader());
            }
            if (name != null) {
                String mail = String.format("%s <%s>",
                        cleanUpName(name),
                        outboundMail.getFrom());

                LOG.trace("Injecting from name: {}", name);

                try  {
                    outboundMail.addHeader("From", mail);
                } catch (Exception e) {
                    LOG.warn("Couldn't inject name in from", e);
                }
            }
        } catch (PersistenceException e) {
            LOG.error("Couldn't read conversation headers. Ignoring message", e);
        }
    }

    @Override
    public int getOrder() {
        return config.getOrder();
    }

    private String cleanUpName(String name) {
        return name;
    }
}
