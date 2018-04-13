package com.ecg.comaas.gtuk.filter.flagged;

import com.codahale.metrics.Timer;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.UserFlaggedFilterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GumtreeUserFlaggedFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Timer TIMER = TimingReports.newTimer("flagged-filter-process-time");

    private Filter pluginConfig;
    private UserFlaggedFilterConfig filterConfig;

    @Value("${gumtree.flagged.header.seller:flagged-seller}")
    private String sellerFlaggedHeader;

    @Value("${gumtree.flagged.header.buyer:flagged-buyer}")
    private String buyerFlaggedHeader;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) {
        try (Timer.Context ignore = TIMER.time()) {
            List<FilterFeedback> reasons = new ArrayList<>();

            Map<String, String> customValues = messageContext.getConversation().getCustomValues();
            String sellerFlagged = customValues.get(sellerFlaggedHeader);
            String buyerFlagged = customValues.get(buyerFlaggedHeader);

            if (sellerFlagged != null || buyerFlagged != null) {
                String shortDescription = "Message blocked due to flag by " + (sellerFlagged != null ? "seller" : "buyer");
                String description = GumtreeFilterUtil.longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), shortDescription);
                reasons.add(new FilterFeedback(messageContext.getMail().get().getFrom(), description, 0, FilterResultState.DROPPED));
            }

            return reasons;
        }
    }

    GumtreeUserFlaggedFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeUserFlaggedFilter withFilterConfig(UserFlaggedFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }

}
