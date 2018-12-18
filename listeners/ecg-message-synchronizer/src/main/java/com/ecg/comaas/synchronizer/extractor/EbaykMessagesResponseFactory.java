package com.ecg.comaas.synchronizer.extractor;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * COPIED FROM EBAYK MESSAGECENTER TO BE ABLE TO PARSE TEMPLATES.
 */
public class EbaykMessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EbaykMessagesResponseFactory.class);

    enum LinebreakAdderRules {

        RULE_1(Pattern.compile("(@mail.ebay-kleinanzeigen.de)([^\n\r>:])"), "$1\n$2"),
        RULE_2(Pattern.compile("[:]\t"), ":\n"),
        RULE_3(Pattern.compile("[:]   "), ":\n"),
        RULE_4(Pattern.compile("    "), "\n"),
        // RULE_5 for weird webmailer client which is prepending the Subject to the text
        RULE_5(Pattern.compile("eBay Kleinanzeigen [|] Kostenlos. Einfach. Lokal. Anzeigen gratis inserieren mit eBay Kleinanzeigen([\\S])"), "$1");

        LinebreakAdderRules(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        private Pattern pattern;
        private String replacement;

        public Pattern getPattern() {
            return pattern;
        }

        public String getReplacement() {
            return replacement;
        }
    }

    private static final Pattern MESSAGEBOX_OFFER_PATTERN = Pattern.compile("Angebot:\\s+[0-9\\.,]+\\s+(EUR|\\\\u20ac|€)\\s+Angebot annehmen\\s+Gegenangebot");
    // not used for push notifications. check PushNotificationTextShortener for that purpose. this impl is more defensive to ensure correct results.
    private static final List<Pattern> REPLACE_PATTERNS = ImmutableList.of(
            Pattern.compile("[<]http:.*?[>]", Pattern.MULTILINE),
            Pattern.compile("[*]", Pattern.MULTILINE),
            Pattern.compile("---* ?Urspr.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("---* ?Original.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("---* ?Reply.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("Anfang der weitergeleiteten .*$", Pattern.MULTILINE),
            Pattern.compile(" hat am.*?geschrieben:", Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("(^| )wrote:", Pattern.MULTILINE),
            Pattern.compile("(^| )schrieb:", Pattern.MULTILINE),
            Pattern.compile("Am [0-9][0-9][.].*? schrieb:?(.*?ber eBay Kleinanzeigen)?", Pattern.MULTILINE),
            Pattern.compile("On [0-9][0-9][.].*? wrote:?(.*?ber eBay Kleinanzeigen)?", Pattern.MULTILINE),
            Pattern.compile("Sent:.*?$", Pattern.MULTILINE),
            Pattern.compile("Gesendet:.*?$", Pattern.MULTILINE),
            Pattern.compile("Von:.*?$", Pattern.MULTILINE),
            Pattern.compile("From:.*?$", Pattern.MULTILINE),
            Pattern.compile("Subject:.*?$", Pattern.MULTILINE),
            Pattern.compile("Betreff:.*?\".*?\"\\s*$", Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("Betreff:.*?$", Pattern.MULTILINE),
            Pattern.compile("Date:.*?$", Pattern.MULTILINE),
            Pattern.compile("Datum:.*?$", Pattern.MULTILINE),
            Pattern.compile("Am:.*?$", Pattern.MULTILINE),
            Pattern.compile("An:.*?$", Pattern.MULTILINE),
            Pattern.compile("To:.*?$", Pattern.MULTILINE),
            Pattern.compile("On[\\W].*?$", Pattern.MULTILINE),
            Pattern.compile("^.*?[<].*?@.*?[>].*?$", Pattern.MULTILINE),
            Pattern.compile("\\<mailto:.*?@.*?\\>"),
            Pattern.compile("\\[mailto:.*?@.*?\\]"),
            Pattern.compile("interessent-.*?@mail.ebay-kleinanzeigen.de:?"),
            Pattern.compile("anbieter-.*?@mail.ebay-kleinanzeigen.de:?"),
            Pattern.compile("[Vv]om [Ii][Pp]hone gesendet"), // no Pattern.CASE_INSENSITIVE switch as it is expensive
            Pattern.compile("[Vv]on meinem [Ii][Pp]hone gesendet"), // no Pattern.CASE_INSENSITIVE switch as it is expensive
            Pattern.compile("(eBay Kleinanzeigen [|] Kosten.*?)?Anfrage zu Ihrer.*?Ein Interessent.*?Anzeigennummer:.*?[0-9]+.*?", Pattern.DOTALL),
            Pattern.compile("^.*?Nachricht von:.*$", Pattern.MULTILINE),
            Pattern.compile("Artikel bereits verkauft.*?[|].*?Kontakt(\\[.*?\\])?", Pattern.DOTALL | Pattern.MULTILINE),
            Pattern.compile("Beantworten Sie diese Nachricht einfach.*?[|].*?Kontakt(\\[.*?\\])?", Pattern.DOTALL | Pattern.MULTILINE),
            MESSAGEBOX_OFFER_PATTERN
    );

    public String getCleanedMessage(Conversation conv, Message message) {
        if (conv.getMessages().get(0).getId().equals(message.getId())) {
            // Initial message
            return cleanupMessage(message.getPlainTextBody().trim());
        } else {
            // Reply messages
            return stripReplyMessage(conv, message);
        }
    }

    private static String removeFromMessageboxReply(String input) {
        return MESSAGEBOX_OFFER_PATTERN.matcher(input).replaceAll("");
    }

    private static String cleanupMessage(String input) {
        String result = input;

        // need to insert some linebreaks as many mail-clients to weird processing of mail-text
        for (LinebreakAdderRules item : LinebreakAdderRules.values()) {
            long start = System.currentTimeMillis();

            Matcher matcher = item.getPattern().matcher(result);
            result = matcher.replaceAll(item.getReplacement()).trim();
            long end = System.currentTimeMillis();
            long duration = end - start;
            if (duration > 20 && LOG.isDebugEnabled()) {
                LOG.debug("Long running regex, Length: " + result.length() + " Time: " + duration + " Pattern: " + item.getPattern().toString());
            }
        }

        for (Pattern pattern : REPLACE_PATTERNS) {
            long start = System.currentTimeMillis();
            Matcher matcher = pattern.matcher(result);
            result = matcher.replaceAll("").trim();
            long end = System.currentTimeMillis();
            long duration = end - start;
            if (duration > 20 && LOG.isDebugEnabled()) {
                LOG.debug("Long running regex, Length: " + result.length() + " Time: " + duration + " Pattern: " + pattern.toString());
            }
        }
        return result;
    }

    private String stripReplyMessage(Conversation conv, Message message) {
        if (contactPosterForExistingConversation(message)) {
            // new contactPoster from payload point of view same as first message of a conversation
            return cleanupMessage(message.getPlainTextBody());
        } else if (comesFromMessageBoxClient(message)) {
            // no need to strip or diff anything, in message-box the whole payload is a additional message
            return removeFromMessageboxReply(message.getPlainTextBody().trim());
        } else {
            int i = conv.getMessages().size() - 1;
            DiffInput left = new DiffInput(lookupMessageToBeDiffedWith(conv.getMessages(), i).getPlainTextBody(), conv.getId(), lookupMessageToBeDiffedWith(conv.getMessages(), i).getId());
            DiffInput right = new DiffInput(message.getPlainTextBody(), conv.getId(), message.getId());

            int textSize = left.getText().length() + right.getText().length();
            if (textSize > 200000) {
                LOG.info("Large message-diff text-set KB: '{}', conv #{}", textSize, conv.getId());
            }

            TextDiffer.TextCleanerResult cleanupResult = diff(left, right);
            return cleanupResult.getCleanupResult();
        }
    }

    private Message lookupMessageToBeDiffedWith(List<Message> messageRts, int i) {
        // unlikely that reply is from own message, rather refer to last message of other party
        MessageDirection messageDirection = messageRts.get(i).getMessageDirection();
        for (int j = 1; i - j >= 0; j++) {
            if (messageDirection == MessageDirection.BUYER_TO_SELLER) {
                if (messageRts.get(i - j).getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
                    return messageRts.get(i - j);
                }
            }
            if (messageDirection == MessageDirection.SELLER_TO_BUYER) {
                if (messageRts.get(i - j).getMessageDirection() == MessageDirection.BUYER_TO_SELLER) {
                    return messageRts.get(i - j);
                }
            }
        }

        // fallback first message tried to be diffed
        return messageRts.get(0);
    }

    private boolean comesFromMessageBoxClient(Message messageRts) {
        return messageRts.getCaseInsensitiveHeaders().containsKey("X-Reply-Channel") &&
                (messageRts.getCaseInsensitiveHeaders().get("X-Reply-Channel").contains("api_") ||
                        messageRts.getCaseInsensitiveHeaders().get("X-Reply-Channel").contains("desktop"));
    }

    private boolean contactPosterForExistingConversation(Message messageRts) {
        return messageRts.getCaseInsensitiveHeaders().containsKey("X-Reply-Channel") &&
                messageRts.getCaseInsensitiveHeaders().get("X-Reply-Channel").startsWith("cp_");
    }

    private TextDiffer.TextCleanerResult diff(DiffInput left, DiffInput right) {
        DiffInput cleanedLeft = new DiffInput(cleanupMessage(left.getText()), left.getConvId(), left.getMessageId());
        DiffInput cleanedRight = new DiffInput(cleanupMessage(right.getText()), right.getConvId(), right.getMessageId());

        TextDiffer leftTextDiffer = new TextDiffer(cleanedLeft);
        TextDiffer rightTextDiffer = new TextDiffer(cleanedRight);

        for (TextDiffer.NGram nGram : leftTextDiffer.getNgramsOfText()) {
            rightTextDiffer.findMatch(nGram);
        }

        return rightTextDiffer.cleanupMatches();
    }


    public static class DiffInput {

        private String text;
        private String convId;
        private String messageId;

        DiffInput(String text, String convId, String messageId) {
            this.text = text;
            this.convId = convId;
            this.messageId = messageId;
        }

        String getText() {
            return text;
        }

        String getConvId() {
            return convId;
        }

        String getMessageId() {
            return messageId;
        }
    }
}