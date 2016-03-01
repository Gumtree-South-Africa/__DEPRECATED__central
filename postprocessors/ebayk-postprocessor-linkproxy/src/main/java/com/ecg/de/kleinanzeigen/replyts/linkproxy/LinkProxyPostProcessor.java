package com.ecg.de.kleinanzeigen.replyts.linkproxy;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashSet;

public class LinkProxyPostProcessor implements PostProcessor {

    private final Escaper escaper;

    private static final Logger LOG = LoggerFactory.getLogger(LinkProxyPostProcessor.class);

    @Autowired
    public LinkProxyPostProcessor(@Value("${replyts.linkescaper.proxyurl}") String referringDomain,
                                  @Value("${replyts.linkescaper.whitelist}") String whitelist) {

        HashSet<String> items = Sets.newHashSet(Splitter.on(',').split(whitelist));
        // support domain for W3C in doctype definitions
        items.add("w3.org");

        this.escaper = new Escaper(referringDomain, ImmutableSet.copyOf(items));
    }


    @Override
    public void postProcess(MessageProcessingContext context) {
        try {

            for (TypedContent<String> contentPart : context.getOutgoingMail().getTextParts(false)) {
                String content = contentPart.getContent();
                String partMediaType = contentPart.getMediaType().toString();
                if (partMediaType.startsWith("text/html")) {
                    contentPart.overrideContent(escaper.escapeHtml(content));
                } else {
                    contentPart.overrideContent(escaper.escapePlaintext(content));
                }
            }
        } catch (RuntimeException e) {
            LOG.error("LinkProxyPostprocessor could not escape links in conversation/mail "+context.getConversation().getId()+"/"+context.getMessageId(), e);
        }

    }

    @Override
    public int getOrder() {
        return 0;
    }
}
