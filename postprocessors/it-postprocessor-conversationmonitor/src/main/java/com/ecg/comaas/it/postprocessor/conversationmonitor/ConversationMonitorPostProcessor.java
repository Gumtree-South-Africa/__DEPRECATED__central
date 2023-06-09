package com.ecg.comaas.it.postprocessor.conversationmonitor;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_IT;

@ComaasPlugin
@Profile(TENANT_IT)
@Component
public class ConversationMonitorPostProcessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationMonitorPostProcessor.class);

    private final List<ReplacedChar> replacedCharsList;

    @Autowired
    public ConversationMonitorPostProcessor(
            @Value("${replyts.conversation.monitor.replaced.chars:}") String replacedChars) {

        this.replacedCharsList = buildReplacedChars(replacedChars);
        LOG.trace("Conversation monitor replaced chars: {}", replacedChars);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();

        List<TypedContent<String>> textParts = outgoingMail.getTextParts(false);
        textParts.forEach(part -> {
            String content = part.getContent();
            content = replaceChars(content);
            part.overrideContent(content);
        });
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private String replaceChars(String content) {
        return replacedCharsList.stream().reduce(content,
                (c, replacedChar) -> c.replace(replacedChar.getToBeReplaced(), replacedChar.getReplacement()),
                (c1, c2) -> c2);
    }

    private List<ReplacedChar> buildReplacedChars(String replacedChars) {
        return Arrays
                .stream(replacedChars.split(","))
                .filter(s -> !StringUtils.isEmpty(s))
                .map(header -> {
                    String[] splitReplacedchar = header.split("\\|");
                    if (splitReplacedchar.length != 2) {
                        LOG.error("Conversation monitor plugin configuration error: replaced chars are not properly configured");
                        throw new IllegalArgumentException(
                                "Conversation monitor plugin configuration error: replaced chars are not properly configured");
                    }
                    return new ReplacedChar(splitReplacedchar[0], splitReplacedchar[1]);
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
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
