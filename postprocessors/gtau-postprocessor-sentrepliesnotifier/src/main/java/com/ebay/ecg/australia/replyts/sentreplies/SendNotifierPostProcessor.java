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

    public static final String ALWAYS_NOTIFY_FLAG = "Always-Notify-Flag";

    private static final String XML_BODY_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private static final Logger LOGGER = LoggerFactory.getLogger(SendNotifierPostProcessor.class);

    private final SendNotifierConfig config;

    @Autowired
    public SendNotifierPostProcessor(@Value("${replyts.sendnotifier.endpoint.url}") String endpointUrl) {
        this.config = new SendNotifierConfig(endpointUrl);
    }

    public void postProcess(MessageProcessingContext context) {
        // Only if the message is from a buyer to a seller
        if (MessageDirection.BUYER_TO_SELLER == context.getMessageDirection()) {
            try {
                Conversation conversation = context.getConversation();

                if (this.isOkToNotify(conversation)) {

                    // check whether this is an XML formatted message used for posting to Autogate
                    final StringBuilder sbBodyText = new StringBuilder();
                    for (TypedContent<String> textPart : context.getOutgoingMail().getTextParts(false)) {
                        sbBodyText.append(textPart.getContent());
                    }
                    if (sbBodyText.toString().contains(XML_BODY_CONTENT)) {
                        LOGGER.info(
                                "Message body contains XML content - ignoring this conversation id [{}], message id [{}] to prevent double counting of replies",
                                conversation.getId(), context.getMessageId());
                    }
                    else {
                        final String notifyUrl = this.buildNotifyUrl(conversation);
                        URL url = new URL(notifyUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        try {
                            connection.setRequestMethod("GET");
                            connection.connect();
                            final int responseCode = connection.getResponseCode();
                            if (HttpURLConnection.HTTP_OK != responseCode) {
                                LOGGER.warn(notifyUrl + ": " + responseCode);
                            }
                        }
                        catch (IOException e) {
                            LOGGER.error("Error notifying " + notifyUrl, e);
                        }
                        finally {
                            connection.disconnect();
                        }
                    }
                }
            }
            catch (Exception ex) {
                LOGGER.error(" Skipping it.", ex);
            }
        }
    }


    private boolean isOkToNotify(Conversation conversation) {

        // If this is the first message from the buyer to the seller
        if (1 == conversation.getMessages().size()) {
            LOGGER.debug("Notify clients as it is the first message in conversation.");
            return true;
        }

        // Check if notify override has been set.
        // In this case always notify the client.
        if (isNotifyOverride(conversation)) {
            LOGGER.debug("Notify clients because 'always notify flag' is set.");
            return true;
        }

        return false;
    }


    private String buildNotifyUrl(Conversation conversation) {
        StringBuilder sb = new StringBuilder(config.getEndpointUrl());
        sb.append("?adId=");
        sb.append(conversation.getAdId());
        // Let them know we were told to always notify in case they want to track it.
        if (isNotifyOverride(conversation)) {
            sb.append("&o=true");
        }
        LOGGER.debug("Notify URL [{}].", sb.toString());
        return sb.toString();
    }

    private boolean isNotifyOverride(Conversation conversation) {
        LOGGER.debug("Conversation custom values [{}].", conversation.getCustomValues());
        Object isNotifyOverrideFlag = conversation.getCustomValues().get(ALWAYS_NOTIFY_FLAG);
        LOGGER.debug("Always notify flag [{}].", isNotifyOverrideFlag);
        return (isNotifyOverrideFlag != null && Boolean.valueOf(isNotifyOverrideFlag.toString()));
    }

    public int getOrder() {
        return 0;
    }

}