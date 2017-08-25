package com.ecg.de.mobile.replyts.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.app.postprocessorchain.postprocessors.Anonymizer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;


/**
 * Replaces the email address TO a seller, if it differs from address of last response of seller in these conversation.
 */
public class SendToLastSellerAddressPostProcessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SendToLastSellerAddressPostProcessor.class);

    @Override
    public void postProcess(MessageProcessingContext context) {

        if (context.getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
            LOG.trace("Message {}: Ignoring seller-to-buyer message direction.", context.getMessageId());
            return;
        }

        final Conversation conversation = context.getConversation();

        String lastFromHeader = getLastFromHeaderOfSellerMessages(conversation);
        if (StringUtils.isEmpty(lastFromHeader)) {
            return;
        }

        try {
            InternetAddress lastAddress = new InternetAddress(lastFromHeader);
            InternetAddress currentAddress = new InternetAddress(conversation.getSellerId());

            if (lastAddress.equals(currentAddress)) {

                LOG.trace("Message {}: Nothing to change", context.getMessageId());

            } else {

                MailAddress newTo = new MailAddress(lastAddress.toString());

                context.getOutgoingMail().setTo(newTo);

                LOG.trace("Message {}: Replaced delivery address {} with last seller address {}.",
                        context.getMessageId(), currentAddress, lastAddress);
            }

        } catch (AddressException e) {
            LOG.warn("Message {}: Invalid Address {}  or {}", context.getMessageId(), lastFromHeader,
                    conversation.getSellerId());
        }
    }

    private static String getLastFromHeaderOfSellerMessages(Conversation c) {
        List<Message> messages = c.getMessages();
        // There must be more than one message (first message is from buyer)
        if (messages == null || messages.size() < 2) {
            return null;
        }

        String fromHeader = null;
        for (Message message : messages) {
            if (message.getMessageDirection() == MessageDirection.SELLER_TO_BUYER) {
                fromHeader = message.getHeaders().get("From");
            }
        }

        return fromHeader;
    }

    /**
     * The order id of this ReceiverManipulator needs to be higher than the order id of the {@link Anonymizer}.
     */
    @Override
    public int getOrder() {
        return 300; // this post processor MUST stay AFTER the Anonymizer post processor!
    }
}
