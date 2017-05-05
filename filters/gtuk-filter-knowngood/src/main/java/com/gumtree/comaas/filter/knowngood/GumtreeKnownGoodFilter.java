package com.gumtree.comaas.filter.knowngood;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import com.gumtree.gumshield.api.client.spec.UserApi;
import com.gumtree.gumshield.api.domain.known_good.KnownGoodResponse;
import com.gumtree.gumshield.api.domain.known_good.KnownGoodStatus;
import com.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.*;

public class GumtreeKnownGoodFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GumtreeKnownGoodFilter.class);
    private static final String MESSAGE = "Sender is known good";
    private static final String BUYERGOOD = "buyergood";
    private static final String SELLERGOOD = "sellergood";
    private final Timer processTimer = TimingReports.newTimer("known-good-filter-process-time");
    private final Counter errorCounter = TimingReports.newCounter("known-good-failed-gumshield-calls");

    private UserApi userApi;
    private Filter pluginConfig;
    private KnownGoodFilterConfig filterConfig;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) throws ProcessingTimeExceededException {
        try (Timer.Context ignore = processTimer.time()) {
            if (hasExemptedCategory(filterConfig.getExemptedCategories(), (Set<Long>) messageContext.getFilterContext().get("categoryBreadCrumb"))) {
                return Collections.emptyList();
            }

            List<FilterFeedback> reasons = new ArrayList<>();
            Boolean knownGood = isKnownGood(messageContext);

            if (knownGood) {
                Mail mail = messageContext.getMail();
                LOGGER.debug("Sender '" + mail.getFrom() + "' is known good");

                String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), MESSAGE);

                reasons.add(new FilterFeedback(mail.getFrom(), description, 0, resultFilterResultMap.get(filterConfig.getResult())));
            }

            return reasons;
        }
    }

    private Boolean isKnownGood(MessageProcessingContext messageContext) {
        Boolean knownGood = false;
        Message message = messageContext.getMessage();
        Long senderId = getSenderId(message.getHeaders(), message.getMessageDirection());
        if (senderId != null) {
            try {
                KnownGoodResponse knownGoodResponse = userApi.knownGood(senderId);
                knownGood = knownGoodResponse != null && knownGoodResponse.getStatus() == KnownGoodStatus.GOOD;
            } catch (RuntimeException e) {
                errorCounter.inc();
                LOGGER.info("Could not get known good status", e);
            }
        } else {
            knownGood = getKnownGood(
                    messageContext.getConversation().getCustomValues(),
                    messageContext.getMessage().getMessageDirection()
            );
        }
        return knownGood;
    }

    private Long getSenderId(Map<String, String> headers, MessageDirection messageDirection) {
        String senderIdHeader = MessageDirection.BUYER_TO_SELLER.equals(messageDirection)
                ? GumtreeCustomHeaders.BUYER_ID.getHeaderValue()
                : GumtreeCustomHeaders.SELLER_ID.getHeaderValue();
        try {
            return Long.parseLong(headers.get(senderIdHeader));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getKnownGood(Map<String, String> customValues, MessageDirection messageDirection) {
        String knownGoodHeader = MessageDirection.BUYER_TO_SELLER.equals(messageDirection) ? BUYERGOOD : SELLERGOOD;
        return customValues.containsKey(knownGoodHeader) && customValues.get(knownGoodHeader) != null;
    }

    GumtreeKnownGoodFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeKnownGoodFilter withFilterConfig(KnownGoodFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }

    GumtreeKnownGoodFilter withUserApi(UserApi userApi) {
        this.userApi = userApi;
        return this;
    }
}