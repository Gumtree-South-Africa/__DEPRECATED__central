package nl.marktplaats.postprocessor.anonymizebody.safetymessage;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.PlainTextMailPartInsertion;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.SafetyTextInsertion;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.HtmlMailPartInsertion;
import nl.marktplaats.postprocessor.anonymizebody.safetymessage.support.MediaTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by reweber on 19/10/15
 */
public class SafetyMessagePostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SafetyMessagePostProcessor.class);
    private SafetyMessagePostProcessorConfig safetyMessagePostProcessorConfig;

    @Autowired
    public SafetyMessagePostProcessor(SafetyMessagePostProcessorConfig safetyMessagePostProcessorConfig) {
        this.safetyMessagePostProcessorConfig = safetyMessagePostProcessorConfig;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        Conversation conversation = context.getConversation();
        Message message = context.getMessage();
        Mail mail = context.getMail();

        LOG.debug("Running subsequent anonymise format post process for message #" + message.getId());

        try {
            // get number of preceding messages
            MessageDirection messageDirection = message.getMessageDirection();
            List<String> safetyTexts = safetyTextsFor(messageDirection);

            // Next message to use has index that is equal to number of messages already sent
            int nextSafetyMessageIndex = currentMessageCountForDirection(conversation, messageDirection);
            if (nextSafetyMessageIndex < safetyTexts.size()) {
                LOG.debug("Adding safety text {} for message {}", nextSafetyMessageIndex, message.getId());
                addSafetyText(message, mail, safetyTexts.get(nextSafetyMessageIndex));

            } else {
                LOG.debug("Already sent {} {} messages, exhausted list of safety texts",
                        nextSafetyMessageIndex, messageDirection);
            }

        } catch (PersistenceException pe) {
            LOG.error("Could not retrieve conversation", pe);
        }
    }

    private List<String> safetyTextsFor(MessageDirection messageDirection) {
        if (messageDirection == MessageDirection.BUYER_TO_SELLER) {
            return safetyMessagePostProcessorConfig.getSafetyTextForSeller();
        } else if (messageDirection == MessageDirection.SELLER_TO_BUYER) {
            return safetyMessagePostProcessorConfig.getSafetyTextForBuyer();
        } else {
            throw new IllegalStateException("Unknown message direction");
        }
    }

    private int currentMessageCountForDirection(Conversation conversation, MessageDirection messageDirection) {
        if (conversation.getMessages() == null) {
            LOG.debug("Conversation " + conversation.getId() + " has no messages");
            return 0;
        }

        //Make sure we do not take the current message into account!
        int count = 0;
        for (Message message : conversation.getMessages().stream().limit(conversation.getMessages().size() - 1).collect(Collectors.toList())) {
            if (message.getState() == MessageState.SENT && message.getMessageDirection() == messageDirection) {
                count += 1;
            }
        }
        LOG.debug("Found {} existing messages of direction {} in conversation", count, messageDirection);
        return count;
    }

    private void addSafetyText(Message message, Mail mail, String safetyMessageText) {
        if (safetyMessageText == null || safetyMessageText.length() == 0) {
            LOG.debug("Skipping, safety message is empty (message {})", message.getId());
            return;
        }

        List<TypedContent<String>> typedContents = mail.getTextParts(false);
        LOG.debug("Message {} has {} typed contents", message.getId(), typedContents.size());

        // Find mutable parts and insert the text
        for (TypedContent<String> typedContent : typedContents) {
            if (typedContent.isMutable()) {
                SafetyTextInsertion insertion = MediaTypeHelper.isHtmlCompatibleType(typedContent.getMediaType())
                        ? new HtmlMailPartInsertion()
                        : new PlainTextMailPartInsertion();

                String existingContent = typedContent.getContent();
                String newContent = insertion.insertSafetyText(existingContent, safetyMessageText);

                // LOG.debug("New text: " + newContent);
                typedContent.overrideContent(newContent);
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
