package com.ecg.gumtree.comaas.filter.blacklist;

import com.codahale.metrics.Timer;
import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.BlacklistFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.*;

@Component
public class GumtreeBlacklistFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeBlacklistFilter.class);

    private static final Timer TIMER = TimingReports.newTimer("blacklist-filter-process-time");

    @Autowired
    private GumshieldApi gumshieldApi;

    private Filter pluginConfig;
    private BlacklistFilterConfig filterConfig;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        try (Timer.Context ignore = TIMER.time()) {
            if (hasExemptedCategory(filterConfig, context)) {
                return Collections.emptyList();
            }

            List<FilterFeedback> reasons = new ArrayList<>();

            // check for account holder
            FilterFeedback feedbackSenderOnBlackList = getFilterFeedbackSenderOnBlackList(context);

            if (feedbackSenderOnBlackList != null) {
                reasons.add(feedbackSenderOnBlackList);
            }

            // check recipient for account holder and blacklist
            String recipientEmail = getConversationRecipientEmail(context.getMessage(), context.getConversation());

            if (isRecipientBlacklisted(context.getMessage(), recipientEmail)) {
                String shortDescription = "Recipient blacklisted";
                String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), shortDescription);

                reasons.add(new FilterFeedback(recipientEmail, description, 0, resultFilterResultMap.get(filterConfig.getResult())));
            }
            return reasons;
        }
    }

    private FilterFeedback getFilterFeedbackSenderOnBlackList(MessageProcessingContext messageContext) {
        Message message = messageContext.getMessage();
        Conversation conversation = messageContext.getConversation();
        Mail mail = messageContext.getMail();
        String ipAddress = message.getHeaders().get(GumtreeCustomHeaders.BUYER_IP.getHeaderValue());

        // check for account holder
        if (!isSenderProAccount(message.getHeaders(), message.getMessageDirection())) {
            // check conversation sender on blacklist
            String senderEmail = getConversationSenderEmail(message, conversation);
            if (isBlacklisted(senderEmail)) {
                return addFilterFeedback(senderEmail);
                // check actual sender on blacklist
            } else if (isBlacklisted(mail.getFrom())) {
                return addFilterFeedback(mail.getFrom());
            } else if (isBlacklistedAttribute(ipAddress, ApiChecklistAttribute.HOST)) {
                return addFilterFeedback(ipAddress);
            }
        }

        return null;
    }

    private FilterFeedback addFilterFeedback(String uiHint) {
        String description = longDescription(getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(),
                "Sender blacklisted");
        return new FilterFeedback(uiHint, description, 0, resultFilterResultMap.get(filterConfig.getResult()));
    }

    private boolean isBlacklisted(String email) {
        String emailDomain = StringUtils.substringAfter(email, "@");
        return isBlacklistedAttribute(email, ApiChecklistAttribute.EMAIL) ||
                isBlacklistedAttribute(emailDomain, ApiChecklistAttribute.EMAIL_DOMAIN);
    }

    private boolean isBlacklistedAttribute(String attribute, ApiChecklistAttribute checklistAttribute) {
        try {
            gumshieldApi.checklistApi().findEntryByValue(ApiChecklistType.BLACK, checklistAttribute, attribute);
            return true;
        } catch (Exception e) {
            LOG.debug("Could not find blacklist entry for " + attribute + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isRecipientBlacklisted(Message message, String recipientEmail) {
        return !isRecipientProAccount(message.getHeaders(), message.getMessageDirection())
                && isBlacklisted(recipientEmail);
    }

    private String getConversationRecipientEmail(Message message, Conversation conversation) {
        ConversationRole toRole = message.getMessageDirection().getToRole();
        return conversation.getUserId(toRole);
    }

    private String getConversationSenderEmail(Message message, Conversation conversation) {
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        return conversation.getUserId(fromRole);
    }

    private boolean isSenderProAccount(Map<String, String> headers, MessageDirection messageDirection) {
        return isProAccount(MessageDirection.BUYER_TO_SELLER.equals(messageDirection), headers);
    }

    private boolean isRecipientProAccount(Map<String, String> headers, MessageDirection messageDirection) {
        return isProAccount(MessageDirection.SELLER_TO_BUYER.equals(messageDirection), headers);
    }

    private boolean isProAccount(boolean checkBuyer, Map<String, String> headers) {
        String isProHeader = checkBuyer
                ? GumtreeCustomHeaders.BUYER_IS_PRO.getHeaderValue()
                : GumtreeCustomHeaders.SELLER_IS_PRO.getHeaderValue();
        if (headers.containsKey(isProHeader) && headers.get(isProHeader) != null) {
            return "true".equals(headers.get(isProHeader));
        } else {
            return isAccountHolder(checkBuyer, headers);
        }
    }

    //TODO delete when buyer/seller_good headers are removed
    private boolean isAccountHolder(boolean checkBuyer, Map<String, String> headers) {
        String knownGoodHeader = checkBuyer
                ? GumtreeCustomHeaders.BUYER_GOOD.getHeaderValue()
                : GumtreeCustomHeaders.SELLER_GOOD.getHeaderValue();
        return headers.containsKey(knownGoodHeader)
                && headers.get(knownGoodHeader) != null
                && headers.get(knownGoodHeader).equals(filterConfig.getAccountHolderHeaderValue());
    }

    public GumtreeBlacklistFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    public GumtreeBlacklistFilter withFilterConfig(BlacklistFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }
}
