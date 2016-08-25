package com.ecg.messagecenter.util;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class MessagePreProcessor {

    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String NEW_LINE_CHARACTER_ESCAPED = "\\n";
    private static final String END_OF_LINE_CHARACTER = "\r";
    private static final String END_OF_LINE_CHARACTER_ESCAPED = "\\r";

    private static final List<Pattern> PATTERNS = Arrays.stream(
            new String[]{
                    "\\n.*(a|b)-.*?@mail.marktplaats.nl.*\\n",
                    "(Aan|To)\\s?:.*?@.*?",
                    "(Subject|Onderwerp)\\s?:.*?",
                    "(Date|Datum)\\s?:.*?",
                    "\\n.*<[^<>\\s]+@gmail.[^<>\\s]+>.*\\n",
                    "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
                    "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
                    "<span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *>[^<>]*(?:<[:a-z]+[^>]*>){0,2}[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>){0,2}",
                    "<b><span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *></b> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
                    "<span[^>]*><b>(From|To|Sender|Receiver|Van|Aan) *: *</b></span *> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
                    "\\b(From|To|Sender|Receiver|Van|Aan) *: *(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
                    "\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){1,6}(<|&lt;)?[^<>\\s]+@[^<>\\s]+((<|&lt;)[^<>\\s]+@[^<>\\s]+(>|&gt;))?(>|&gt;)?",
                    "\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){0,5}([^<>\\s]+)(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
                    "Op.{10,25}schreef[^<]{5,60}<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
                    "Op.{10,25}schreef[^<]{5,60}(<|&lt;)?\\s*[^<>\\s]+@[^<>\\s]+(>|&gt;)?"})
            .map(Pattern::compile)
            .collect(Collectors.toList());

    private static final Pattern cssPattern = Pattern.compile("(#outlook a \\{.*?\\}.*?(\\n|\\r\\n))|( blockquote.+?!important; })");

    static String removeEmailClientReplyFragment(Conversation conversation, Message message) {
        String buyerName = nullToEmpty(conversation.getCustomValues().get("buyer-name"));
        String sellerName = nullToEmpty(conversation.getCustomValues().get("seller-name"));
        String replyToUserName = MessageDirection.BUYER_TO_SELLER.equals(message.getMessageDirection()) ? sellerName : buyerName;
        String regexViaMarktplaats = "\\n.*?" + Pattern.quote(replyToUserName) + " via Marktplaats(\\s|\").*\\r?\\n";
        String msgStripped = unescapeNewLines(message.getPlainTextBody()).split(regexViaMarktplaats, 2)[0];

        for (Pattern pattern : PATTERNS) {
            msgStripped = pattern.split(msgStripped, 2)[0];
        }

        return cssPattern.matcher(msgStripped).replaceFirst("").trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String unescapeNewLines(String value) {
        String res = value.replaceAll(NEW_LINE_CHARACTER_ESCAPED, NEW_LINE_CHARACTER);
        return res.replaceAll(END_OF_LINE_CHARACTER_ESCAPED, END_OF_LINE_CHARACTER);
    }
}
