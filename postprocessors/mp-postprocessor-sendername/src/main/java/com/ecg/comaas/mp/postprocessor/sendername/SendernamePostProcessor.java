package com.ecg.comaas.mp.postprocessor.sendername;

import com.ecg.replyts.app.postprocessorchain.EmailPostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;

/**
 * Copies the name from the initial e-mail to the From header for outgoing e-mails.
 * Depends on the information being present in the conversation custom values.
 * </p>
 * The following conversation custom headers are expected:
 * <ol>
 *     <li><b>to</b>: {@code respondent username}</li>
 *     <li><b>from</b>: {@code initiator username}</li>
 * </ol>
 *
 * <p/>
 * If the conversation header is not found, the username is <i>not</i> updated.
 *
 * @author Erik van Oosten
 */
// TODO akobiakov and this one as well
@ComaasPlugin
@Component
public class SendernamePostProcessor implements EmailPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SendernamePostProcessor.class);

    /** Conversation-custom-value to use when mail is from buyer, e.g. initiator of conversation. */
    private static final String CUSTOM_VALUE_NAME_FOR_SELLER = "to";
    /** Conversation-custom-value to use when mail is from seller, e.g. respondent of conversation. */
    private  static final String CUSTOM_VALUE_NAME_FOR_BUYER = "from";

    private final String buyerNamePattern;
    private final String sellerNamePattern;

    @Autowired
    public SendernamePostProcessor(
            @Value("${sendername.buyer.name.pattern}") String buyerNamePattern,
            @Value("${sendername.seller.name.pattern}") String sellerNamePattern) {
        Assert.isTrue(buyerNamePattern != null && buyerNamePattern.contains("%s"));
        Assert.isTrue(sellerNamePattern != null && sellerNamePattern.contains("%s"));

        this.buyerNamePattern = buyerNamePattern;
        this.sellerNamePattern = sellerNamePattern;
    }

    @Override
    public void postProcess(MessageProcessingContext ctx) {
        Conversation conversation = ctx.getConversation();
        Message message = ctx.getMessage();
        MutableMail outboundMail = ctx.getOutgoingMail();

        MessageDirection messageDirection = message.getMessageDirection();
        String senderUserName = conversation.getCustomValues().get(getCustomValueName(messageDirection));
        String formattedSenderName = trimToEmpty(String.format(getPattern(messageDirection), trimToEmpty(senderUserName)));

        String originalFrom = outboundMail.getFrom();
        String newFrom = smtpSafeEmailAddress(originalFrom, formattedSenderName, conversation.getId() + "-" + message.getId());
        LOG.trace("New From: {}", newFrom);
        replaceFromHeader(outboundMail, newFrom);
        LOG.trace("New Mail's From Header: {}", outboundMail.getFrom());
    }

    @Override
    public int getOrder() {
        return 600;
    }

    private String getCustomValueName(MessageDirection md) {
        switch (md) {
            case BUYER_TO_SELLER:
                return CUSTOM_VALUE_NAME_FOR_BUYER;
            case SELLER_TO_BUYER:
                return CUSTOM_VALUE_NAME_FOR_SELLER;
            default:
                throw new IllegalStateException("Unknown message direction " + md);
        }
    }

    private String getPattern(MessageDirection messageDirection) {
        switch (messageDirection) {
            case BUYER_TO_SELLER:
                return buyerNamePattern;
            case SELLER_TO_BUYER:
                return sellerNamePattern;
            default:
                throw new IllegalStateException("Unknown message direction " + messageDirection);
        }
    }

    private String smtpSafeEmailAddress(String originalFrom, String formattedSenderName, String messageId) {
        try {
            return (new InternetAddress(originalFrom, formattedSenderName)).toString();
        } catch (UnsupportedEncodingException e) {
            LOG.error("Could not process message {}", messageId, e);
            throw new RuntimeException(e);
        }
    }

    private void replaceFromHeader(MutableMail mail, String newFrom) {
        mail.removeHeader(Mail.FROM);
        mail.addHeader(Mail.FROM, newFrom);
    }

    private String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

}
