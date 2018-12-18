package com.ecg.comaas.mde.filter.fsbofraud;


import com.ecg.comaas.mde.filter.fsbofraud.broker.MessageBrokerClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * filter that connects to a belen/box style database and queries for the user's
 * state. if the user's state is <code>BLOCKED</code>
 */
class FsboFraudFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(FsboFraudFilter.class);
    private static final String AD_ID_PREFIX = "COMA";
    private final AdChecker adChecker;
    private final MessageBrokerClient messageBrokerClient;

    public FsboFraudFilter(AdChecker adChecker, MessageBrokerClient messageBrokerClient) {
        this.adChecker = adChecker;
        this.messageBrokerClient = messageBrokerClient;
    }

    @Override
    public List<FilterFeedback> filter(
            MessageProcessingContext messageProcessingContext) {

        if (!isFsboSeller(messageProcessingContext)) {
            return Collections.emptyList();
        }

        MessageDirection messageDirection = messageProcessingContext
                .getMessageDirection();

        String senderMailAddress = messageProcessingContext.getConversation()
                .getUserIdFor(messageDirection.getFromRole());
        String adIdString = messageProcessingContext.getConversation()
                .getAdId();
        LOG.trace("filtering {} {}", senderMailAddress, adIdString);
        Long adId = adIdStringToAdId(adIdString);
        LOG.trace("adId {}", adId);
        if (!isValidAdId(adId) || adChecker.isFraud(adId)) {
            return ImmutableList.of(new FilterFeedback(
                    "BLOCKED", "User is blocked " + senderMailAddress, 0,
                    FilterResultState.DROPPED));
        }
        messageBrokerClient.messageSend(messageProcessingContext, adId);

        return Collections.emptyList();
    }

    private static boolean isFsboSeller(MessageProcessingContext messageProcessingContext) {
        String sellerType = messageProcessingContext.getConversation().getCustomValues().get("seller_type");
        LOG.trace("sellerType {}", sellerType);
        if (!StringUtils.hasText(sellerType)) {
            sellerType = messageProcessingContext.getMessage().getCaseInsensitiveHeaders().get("X-Cust-Seller_Type");
        }
        LOG.trace("sellerType {}", sellerType);
        return "FSBO".equals(sellerType);
    }

    private static boolean isValidAdId(Long adId) {
        return null != adId && adId > 0;
    }

    private static Long adIdStringToAdId(String adIdString) {
        if (hasText(adIdString)) {
            return toLong(stripPrefix(adIdString));
        }
        return -1L;
    }

    private static String stripPrefix(String adIdString) {
        if (adIdString.startsWith(AD_ID_PREFIX)) {
            return adIdString.replace(AD_ID_PREFIX, "");
        }
        return adIdString;
    }

    private static Long toLong(String string) {
        if (hasText(string)) {
            return Long.valueOf(string);
        }
        return -1L;
    }
}
