package com.ebay.ecg.australia.replyts.sentreplies;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author mdarapour
 */
public class SendNotifierPostProcessor implements PostProcessor {
    private static final String XML_BODY_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final Logger LOGGER = LoggerFactory.getLogger(SendNotifierPostProcessor.class);

    private final SendNotifierConfig config;

    @Autowired
    public SendNotifierPostProcessor(@Value("${replyts.sendnotifier.endpoint.url}") String endpointUrl) {
        this.config = new SendNotifierConfig(endpointUrl);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        // Only if the message is from a buyer to a seller
        if (MessageDirection.BUYER_TO_SELLER == context.getMessageDirection()) {
            try {
                Conversation conversation = context.getConversation();
                // If this is the first message from the buyer to the seller
                if (1 == conversation.getMessages().size()) {
                    // check whether this is an XML formatted message used for posting to Autogate
                    final StringBuilder sbBodyText = new StringBuilder();
                    for (TypedContent<String> textPart : context.getOutgoingMail().getTextParts(false)) {
                        sbBodyText.append(textPart.getContent());
                    }
                    if (sbBodyText.toString().contains(XML_BODY_CONTENT)) {
                        LOGGER.info("Message body contains XML content - ignoring this conversation id " + conversation.getId() + ", message id " + context.getMessageId() + " to prevent double counting of replies");
                    } else {
                        final String notifyUrl = config.getEndpointUrl() + "?adId=" + conversation.getAdId();
                        URL url = new URL(notifyUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        try {
                            connection.setRequestMethod("GET");
                            connection.connect();
                            final int responseCode = connection.getResponseCode();
                            if (HttpURLConnection.HTTP_OK != responseCode) {
                                LOGGER.warn(notifyUrl + ": " + responseCode);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error notifying " + notifyUrl, e);
                        } finally {
                            connection.disconnect();
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(" Skipping it.", ex);
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
