package com.ecg.messagecenter.util;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;

import java.util.regex.Pattern;

public class MessagePreProcessor {

    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String NEW_LINE_CHARACTER_ESCAPED = "\\\\n";
    private static final String END_OF_LINE_CHARACTER = "\r";
    private static final String END_OF_LINE_CHARACTER_ESCAPED = "\\\\r";

    public static String removeEmailClientReplyFragment(Conversation conversation, Message message) {
        String buyerName = nullToEmpty(conversation.getCustomValues().get("buyer-name"));
        String sellerName = nullToEmpty(conversation.getCustomValues().get("seller-name"));
        String replyToUserName = MessageDirection.BUYER_TO_SELLER.equals(message.getMessageDirection()) ? sellerName : buyerName;
        String regexViaMarktplaats = ".*?" + Pattern.quote(replyToUserName) + " via Marktplaats(\\s|\").*?";
        String regexFromEmail = ".*?(a|b)-.*?@mail.marktplaats.nl.*?";
        String regexToEmail = "(Aan|To)\\s?:.*?@.*?";
        String regexSubjectEmail = "(Subject|Onderwerp)\\s?:.*?";
        String regexDateEmail = "(Date|Datum)\\s?:.*?";
        String[] msgSplit = unescapeNewLines(message.getPlainTextBody()).split(regexViaMarktplaats);
        msgSplit = msgSplit[0].split(regexFromEmail);
        msgSplit = msgSplit[0].split(regexToEmail);
        msgSplit = msgSplit[0].split(regexDateEmail);
        msgSplit = msgSplit[0].split(regexSubjectEmail);
        return msgSplit[0].trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String unescapeNewLines(String value) {
        String res = value.replaceAll(NEW_LINE_CHARACTER_ESCAPED, NEW_LINE_CHARACTER);
        return res.replaceAll(END_OF_LINE_CHARACTER_ESCAPED, END_OF_LINE_CHARACTER);
    }
}