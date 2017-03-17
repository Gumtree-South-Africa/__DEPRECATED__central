package com.ecg.de.kleinanzeigen.replyts.buyeralias;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;

/**
 * User: acharton
 * Date: 11/12/13
 */
class BuyerAliasHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BuyerAliasHandler.class);
    private static final String CUST_HEADER_BUYER_NAME = "buyer-name";
    private static final String CUST_HEADER_SELLER_NAME = "seller-name";

    private final MessageProcessingContext messageProcessingContext;
    private final String buyerHeaderName;
    private final String aliasFormatPattern;
    private final String sellerHeaderName;

    BuyerAliasHandler(MessageProcessingContext messageProcessingContext, String aliasFormatPattern) {
        this(messageProcessingContext, CUST_HEADER_BUYER_NAME, CUST_HEADER_SELLER_NAME, aliasFormatPattern);
    }

    BuyerAliasHandler(MessageProcessingContext messageProcessingContext, String buyerHeaderName, String sellerHeaderName, String aliasFormatPattern) {
        this.messageProcessingContext = messageProcessingContext;
        this.buyerHeaderName = buyerHeaderName;
        this.sellerHeaderName = sellerHeaderName;
        this.aliasFormatPattern = aliasFormatPattern;
    }

    public void handle() {
        try {

            MessageDirection direction = messageProcessingContext.getMessageDirection();

            Optional<String> aliasName = Optional.absent();
            if (direction == MessageDirection.BUYER_TO_SELLER && containsBuyerName()) {
                aliasName = Optional.of(buildAlias(buyerHeaderName));
            } else if (direction == MessageDirection.SELLER_TO_BUYER && containsSellerName()) {
                aliasName = Optional.of(buildAlias(sellerHeaderName));
            }

            if (aliasName.isPresent()) {
                String senderMail = messageProcessingContext.getOutgoingMail().getFrom();
                messageProcessingContext.getOutgoingMail().setFrom(new MailAddress(formatMailAddress(aliasName.get(), senderMail)));
            }
        } catch (Exception e) {
            LOG.error("Error while setting sender alias name.", e);
        }
    }


    private String buildAlias(String headerName) {
        return String.format(this.aliasFormatPattern, messageProcessingContext.getConversation().getCustomValues().get(headerName));
    }


    private String formatMailAddress(String name, String addr) throws UnsupportedEncodingException {
        return new InternetAddress(addr, name).toString();
    }


    boolean containsBuyerName() {
        return messageProcessingContext.getConversation().getCustomValues().containsKey(buyerHeaderName);
    }

    boolean containsSellerName() {
        return messageProcessingContext.getConversation().getCustomValues().containsKey(sellerHeaderName);
    }
}
