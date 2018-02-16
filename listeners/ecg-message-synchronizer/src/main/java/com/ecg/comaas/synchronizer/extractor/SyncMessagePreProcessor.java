package com.ecg.comaas.synchronizer.extractor;

import com.ecg.replyts.core.EnvironmentSupport;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Copied class from MessageBox {@link com.ecg.messagebox.util.MessagePreProcessor}
 */
@Component
public class SyncMessagePreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SyncMessagePreProcessor.class);
    private static final String PREFIX_MESSAGE_ANONYMIZATION_PATTERN = "message.anonymization.pattern.";
    private static final String PREFIX_MESSAGE_NORMALIZATION_PATTERN = "message.normalization.pattern.";

    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String NEW_LINE_CHARACTER_ESCAPED = "\\n";
    private static final String END_OF_LINE_CHARACTER = "\r";
    private static final String END_OF_LINE_CHARACTER_ESCAPED = "\\r";

    private static final Pattern cssPattern = Pattern.compile("(#outlook a \\{.*?\\}.*?(\\n|\\r\\n))|( blockquote.+?!important; })");
    private static final String HTML_LINE_BREAKER = "€€linebreak€€";

    private List<Pattern> patterns;

    @Value("${message.normalization.strip.html.tags:false}")
    private boolean stripHtmlTagsEnabled;

    @Autowired
    public SyncMessagePreProcessor(AbstractEnvironment environment) {
        this.patterns = EnvironmentSupport.propertyNames(environment)
                .stream()
                .filter(key -> key.startsWith(PREFIX_MESSAGE_ANONYMIZATION_PATTERN) || key.startsWith(PREFIX_MESSAGE_NORMALIZATION_PATTERN))
                .sorted((key1, key2) -> {
                    if (key1.startsWith(PREFIX_MESSAGE_ANONYMIZATION_PATTERN) && key2.startsWith(PREFIX_MESSAGE_ANONYMIZATION_PATTERN)) {
                        return Integer.parseInt(key1.substring(PREFIX_MESSAGE_ANONYMIZATION_PATTERN.length())) -
                                Integer.parseInt(key2.substring(PREFIX_MESSAGE_ANONYMIZATION_PATTERN.length()));
                    } else if (key1.startsWith(PREFIX_MESSAGE_NORMALIZATION_PATTERN) && key2.startsWith(PREFIX_MESSAGE_NORMALIZATION_PATTERN)) {
                        return Integer.parseInt(key1.substring(PREFIX_MESSAGE_NORMALIZATION_PATTERN.length())) -
                                Integer.parseInt(key2.substring(PREFIX_MESSAGE_NORMALIZATION_PATTERN.length()));
                    } else {
                        return key1.compareTo(key2);
                    }
                })
                .map(environment::getProperty)
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public String removeEmailClientReplyFragment(Conversation conversation, Message message) {
        String buyerName = nullToEmpty(conversation.getCustomValues().get("buyer-name"));
        String sellerName = nullToEmpty(conversation.getCustomValues().get("seller-name"));
        return removeEmailClientReplyFragment(buyerName, sellerName, message.getPlainTextBody(), message.getMessageDirection());
    }

    public String removeEmailClientReplyFragment(String buyerName, String sellerName, String msgText, MessageDirection msgDirection) {
        String replyToUserName = MessageDirection.BUYER_TO_SELLER.equals(msgDirection) ? sellerName : buyerName;
        String regexViaMarktplaats = "\\n.*?" + Pattern.quote(replyToUserName) + " via Marktplaats(\\s|\").*\\r?\\n";
        String msgStripped = unescapeNewLines(msgText).split(regexViaMarktplaats, 2)[0];

        for (Pattern pattern : patterns) {
            msgStripped = pattern.split(msgStripped, 2)[0];
        }

        String result = cssPattern.matcher(msgStripped).replaceFirst("");

        if (stripHtmlTagsEnabled) {
            result = stripHtmlTags(result);
        }

        return result.trim();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String unescapeNewLines(String value) {
        String res = value.replaceAll(NEW_LINE_CHARACTER_ESCAPED, NEW_LINE_CHARACTER);
        return res.replaceAll(END_OF_LINE_CHARACTER_ESCAPED, END_OF_LINE_CHARACTER);
    }

    private String stripHtmlTags(final String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        try {
            final String in = value.replaceAll("\n", HTML_LINE_BREAKER); // preserve text line breaks
            final Document asHtml = Jsoup.parse(in);
            asHtml.select("br").append(HTML_LINE_BREAKER); // preserve html line breaks

            String text = asHtml.text(); // text without any html code
            text = text.replaceAll(HTML_LINE_BREAKER, "\n"); // text with line breaks
            return text;
        } catch (Exception e) {
            LOG.error("stripping HTML tags failed: {}", e.getMessage(), e);
            return value;
        }
    }
}