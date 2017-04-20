package com.ebay.columbus.replyts2.conversationmonitor;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by fmaffioletti on 7/31/14.
 */
public class ConversationMonitorFilterPostProcessor implements PostProcessor {
    private static final Logger LOG =
                    LoggerFactory.getLogger(ConversationMonitorFilterPostProcessor.class);

    private final List<ReplacedChar> replacedCharsList;

    public ConversationMonitorFilterPostProcessor(String replacedChars) {
        this.replacedCharsList = buildReplacedChars(replacedChars);
        LOG.debug("Conversation monitor replaced chars: " + replacedChars);
    }

    @Override public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();

        List<TypedContent<String>> textParts = outgoingMail.getTextParts(false);
        textParts.stream().forEach(part -> {
            String content = part.getContent();
            content = replaceChars(content);
            part.overrideContent(content);
        });
    }

    @Override public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private String replaceChars(String content) {
        return replacedCharsList.stream().reduce(content,
                (c, replacedChar) -> c.replace(replacedChar.getToBeReplaced(), replacedChar.getReplacement()),
                (c1, c2) -> c2);
    }

    private List<ReplacedChar> buildReplacedChars(String replacedChars) {
        return Arrays.stream(replacedChars.split(",")).filter(s -> StringUtils.isEmpty(s)).map(header -> {
            String[] splitReplacedchar = header.split("\\|");
            if (splitReplacedchar.length != 2) {
                LOG.error("Conversation monitor plugin configuration error: replaced chars are not properly configured");
                throw new IllegalArgumentException(
                        "Conversation monitor plugin configuration error: replaced chars are not properly configured");
            }
            return new ReplacedChar(splitReplacedchar[0], splitReplacedchar[1]);
        }).collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    private class ReplacedChar {

        private String toBeReplaced;
        private String replacement;

        public ReplacedChar(String toBeReplaced, String replacement) {
            this.toBeReplaced = toBeReplaced;
            this.replacement = replacement;
        }

        public String getToBeReplaced() {
            return toBeReplaced;
        }

        public String getReplacement() {
            return replacement;
        }
    }

}
