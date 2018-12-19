package com.ecg.comaas.gtuk.postprocessor.bodyanonymizer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by reweber on 08/10/15
 */
public class RevealEmailHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RevealEmailHandler.class);

    private final MessageBodyAnonymizerConfig messageBodyAnonymizerConfig;

    public RevealEmailHandler(MessageBodyAnonymizerConfig messageBodyAnonymizerConfig) {
        this.messageBodyAnonymizerConfig = messageBodyAnonymizerConfig;
    }

    public void process(Conversation conversation, Message message, Mail mail) {

        if (conversation != null) {
            if (shouldRevealEmail(message.getCaseInsensitiveHeaders())
                    && isInitialB2S(message, conversation)
                    && isKnownGood(message.getCaseInsensitiveHeaders(), message.getMessageDirection())) {

                List<TypedContent<String>> typedContents = mail.getTextParts(false);
                String formatText = messageBodyAnonymizerConfig.getKnownGoodInsertFooterFormat();
                String buyerEmail = conversation.getBuyerId();
                String textToInsert = String.format(formatText, buyerEmail);
                LOG.trace("Buyer email reveal required. Inserting text: {}", textToInsert);

                // Find the first mutable content & do the replacement.
                for (TypedContent<String> typedContent : typedContents) {
                    if (typedContent.isMutable()) {
                        LOG.trace("Inserting buyer email reveal text into message #{}", message.getId());
                        String existingContent = typedContent.getContent();
                        String newContent;

                        if (MediaTypeHelper.isHtmlCompatibleType(typedContent.getMediaType())) {
                            LOG.trace("Content type is HTML. Doing cleverer insert...");
                            textToInsert = textToInsert.replaceAll("\n", "<br>");
                            newContent = existingContent + "<html><br><p>" + textToInsert + "</p></html>";
                        } else {
                            LOG.trace("Content type is plain text. Doing standard insert...");
                            textToInsert = textToInsert.replaceAll("<br>", "\n");
                            newContent = existingContent + "\n\n" + textToInsert;
                        }

                        typedContent.overrideContent(newContent);
                    }
                }
            }
        }
    }

    private boolean shouldRevealEmail(Map<String, String> headers) {
        String revealEmailHeader = messageBodyAnonymizerConfig.getRevealEmailHeader();
        String revealEmailValue = messageBodyAnonymizerConfig.getRevealEmailValue();
        if (revealEmailValue != null && headers.containsKey(revealEmailHeader) && headers.get(revealEmailHeader) != null) {
            return revealEmailValue.equals(headers.get(revealEmailHeader));
        }
        return false;
    }

    /*
     * Note: If the  logic changes such that the 'first message' does not have the same date as the conversation
     * create date, then this needs to change but also change it in the URL filter which applies the same logic.
     */
    private boolean isInitialB2S(Message message, Conversation conversation) {
        DateTime convDate = conversation.getCreatedAt();
        DateTime messageDate = message.getReceivedAt();

        return convDate.compareTo(messageDate) > -1;
    }


    private boolean isKnownGood(Map<String, String> headers, MessageDirection messageDirection) {
        String knownGoodValue = messageBodyAnonymizerConfig.getSellerKnownGoodValue();
        String knownGoodHeader = MessageDirection.BUYER_TO_SELLER.equals(messageDirection)
                ? messageBodyAnonymizerConfig.getSellerKnownGoodHeader()
                : "";
        if (knownGoodValue != null && headers.containsKey(knownGoodHeader) && headers.get(knownGoodHeader) != null) {
            return knownGoodValue.equals(headers.get(knownGoodHeader));
        }
        return false;
    }
}
