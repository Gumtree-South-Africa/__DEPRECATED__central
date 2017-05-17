package com.ecg.messagebox.util;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.EnvironmentSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MessagePreProcessor {
    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String NEW_LINE_CHARACTER_ESCAPED = "\\n";
    private static final String END_OF_LINE_CHARACTER = "\r";
    private static final String END_OF_LINE_CHARACTER_ESCAPED = "\\r";

    private static final Pattern cssPattern = Pattern.compile("(#outlook a \\{.*?\\}.*?(\\n|\\r\\n))|( blockquote.+?!important; })");

    private List<Pattern> patterns;

    @Value("${message.normalization.strip.html.tags:false}")
    private boolean stripHtmlTagsEnabled;

    @Autowired
    public MessagePreProcessor(AbstractEnvironment environment) {
        this.patterns = EnvironmentSupport.propertyNames(environment)
          .stream()
          .filter(key -> key.startsWith("message.normalization.pattern."))
          .sorted((key1, key2) -> {
            int position1 = Integer.parseInt(key1.substring("message.normalization.pattern.".length()));
            int position2 = Integer.parseInt(key2.substring("message.normalization.pattern.".length()));
            return position1 - position2;
          })
          .map(key -> environment.getProperty(key))
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

    private String stripHtmlTags(String value) {
        return Jsoup.clean(value, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }
}