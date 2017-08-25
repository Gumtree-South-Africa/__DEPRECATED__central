package com.gumtree.replyts2.anonymizer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by reweber on 08/10/15
 */
public class SafetyTextHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SafetyTextHandler.class);

    private MessageBodyAnonymizerConfig messageBodyAnonymizerConfig;

    public SafetyTextHandler(MessageBodyAnonymizerConfig messageBodyAnonymizerConfig) {
        this.messageBodyAnonymizerConfig = messageBodyAnonymizerConfig;
    }

    public void process(Conversation conversation, Message message, Mail mail) {

        LOG.trace("Running subsequent anonymise format post process for message #" + message.getId());

        // only apply to messages from seller to buyer
        if (MessageDirection.SELLER_TO_BUYER.equals(message.getMessageDirection())) {

            if (isFirstSendableMessageFromSeller(conversation)) {
                LOG.trace("Format text is required for message {}", message.getId());
                String formatText = messageBodyAnonymizerConfig.getSafetyTextFormat();

                Map<String, String> headers = message.getHeaders();

                LOG.trace("Conversation headers: {}", headers);

                if (shouldRevealBuyerEmail(headers) && isKnownGood(headers)) {
                    formatText = messageBodyAnonymizerConfig.getKnownGoodSellerSafetyTextFormat();
                }

                String textToInsert = String.format(formatText, mail.getFrom());
                LOG.trace("Safety text to insert: " + textToInsert);
                List<TypedContent<String>> typedContents = mail.getTextParts(false);

                // Find the first mutable content & do the replacement.
                for (TypedContent<String> typedContent : typedContents) {
                    if (typedContent.isMutable()) {
                        LOG.trace("Inserting safety text into message #{}", message.getId());
                        String existingContent = typedContent.getContent();
                        String newContent;

                        if (MediaTypeHelper.isHtmlCompatibleType(typedContent.getMediaType())) {
                            LOG.trace("Content type is HTML. Doing cleverer insert...");
                            textToInsert = textToInsert.replaceAll("\n", "<br>");
                            newContent = "<html><p>" + textToInsert + "</p><br></html>" + existingContent;
                        } else {
                            LOG.trace("Content type is plain text. Doing standard insert...");
                            textToInsert = textToInsert.replaceAll("<br>", "\n");
                            newContent = textToInsert + "\n\n" + existingContent;
                        }

                        typedContent.overrideContent(newContent);
                    }
                }
            }
        }
    }

    private boolean shouldRevealBuyerEmail(Map<String, String> headers) {
        String revealEmailHeader = messageBodyAnonymizerConfig.getRevealEmailHeader();
        String revealEmailValue = messageBodyAnonymizerConfig.getRevealEmailValue();
        if (revealEmailValue != null && headers.containsKey(revealEmailHeader) && headers.get(revealEmailHeader) != null) {
            return revealEmailValue.equals(headers.get(revealEmailHeader));
        }
        return false;
    }

    private boolean isFirstSendableMessageFromSeller(Conversation conversation) {
        if (conversation.getMessages() == null) {
            LOG.trace("Conversation {} has no messages", conversation.getId());
            return false;
        }
        return !conversation.getMessages().stream()
                .limit(conversation.getMessages().size() - 1)
                .anyMatch(message ->
                        MessageState.SENT.equals(message.getState()) &&
                                MessageDirection.SELLER_TO_BUYER.equals(message.getMessageDirection())
                );
    }

    private boolean isKnownGood(Map<String, String> headers) {
        String knownGoodValue = messageBodyAnonymizerConfig.getSellerKnownGoodValue();
        String knownGoodHeader = messageBodyAnonymizerConfig.getSellerKnownGoodHeader();
        if (knownGoodValue != null && headers.containsKey(knownGoodHeader) && headers.get(knownGoodHeader) != null) {
            return knownGoodValue.equals(headers.get(knownGoodHeader));
        }
        return false;
    }
}
