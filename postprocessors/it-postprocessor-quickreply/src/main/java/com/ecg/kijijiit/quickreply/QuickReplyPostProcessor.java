package com.ecg.kijijiit.quickreply;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fmaffioletti on 7/31/14.
 */
public class QuickReplyPostProcessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(QuickReplyPostProcessor.class);

    private final List<String> resolvedPlaceholders;

    public QuickReplyPostProcessor(String placeholders) {
        Preconditions.checkNotNull(placeholders, "placeholders cannot be null, at least it must be an empty list");
        this.resolvedPlaceholders = Lists.newArrayList(Splitter.on(",").trimResults().omitEmptyStrings().split(placeholders));
        LOG.debug("QuickReply resolved placeholders: " + resolvedPlaceholders);
        Preconditions.checkNotNull(this.resolvedPlaceholders, "resolvedPlaceholders cannot be null");
    }

    @Override public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();
        outgoingMail.getTextParts(false).stream()
                .forEach(part -> part.overrideContent(replacePlaceholders(part.getContent(), outgoingMail.getFrom())));
    }

    @Override public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private String replacePlaceholders(String content, String replacement) {
        return resolvedPlaceholders.stream()
                .reduce(content, (c, placeholder)-> c.replace(placeholder, replacement));
    }

}
