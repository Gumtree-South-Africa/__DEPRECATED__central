package com.ecg.gumtree.comaas.filter.watchlist;

import com.codahale.metrics.Timer;
import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import com.gumtree.gumshield.api.client.spec.ChecklistApi;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.*;

public class GumtreeWatchlistFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeWatchlistFilter.class);

    private static final Timer TIMER = TimingReports.newTimer("watchlist-filter-process-time");

    private Filter pluginConfig;
    private WatchlistFilterConfig watchListFilterConfig;
    private ChecklistApi checklistApi;

    private static final String SHORT_DESCRIPTION = "Sender on watchlist";

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) throws ProcessingTimeExceededException {
        try (Timer.Context ignored = TIMER.time()) {
            if (hasExemptedCategory(watchListFilterConfig, messageContext)) {
                return Collections.emptyList();
            }

            List<FilterFeedback> reasons = new ArrayList<>();
            Mail mail = messageContext.getMail();
            String ipAddress = messageContext.getMessage().getHeaders()
                    .get(GumtreeCustomHeaders.BUYER_IP.getHeaderValue());
            String emailDomain = StringUtils.substringAfter(mail.getFrom(), "@");

            if (isWatchlisted(mail.getFrom(), ApiChecklistAttribute.EMAIL)) {
                addFilterFeedback(watchListFilterConfig, reasons, mail.getFrom());
            } else if (isWatchlisted(emailDomain, ApiChecklistAttribute.EMAIL_DOMAIN)) {
                addFilterFeedback(watchListFilterConfig, reasons, emailDomain);
            } else if (isWatchlisted(ipAddress, ApiChecklistAttribute.HOST)) {
                addFilterFeedback(watchListFilterConfig, reasons, ipAddress);
            }

            return reasons;
        }
    }

    private void addFilterFeedback(WatchlistFilterConfig watchListFilterConfig, List<FilterFeedback> reasons, String uiHint) {
        String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), watchListFilterConfig.getVersion(), SHORT_DESCRIPTION);
        reasons.add(new FilterFeedback(uiHint, description, 0, resultFilterResultMap.get(watchListFilterConfig.getResult())));
    }

    private boolean isWatchlisted(String attribute, ApiChecklistAttribute checklistAttribute) {
        try {
            checklistApi.findEntryByValue(ApiChecklistType.WATCH, checklistAttribute, attribute);
            return true;
        } catch (Exception e) {
            LOG.debug("Could not find watchlist entry for " + attribute + ": " + e.getMessage());
            return false;
        }
    }

    GumtreeWatchlistFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeWatchlistFilter withFilterConfig(WatchlistFilterConfig filterConfig) {
        this.watchListFilterConfig = filterConfig;
        return this;
    }

    GumtreeWatchlistFilter withChecklistApi(ChecklistApi checklistApi) {
        this.checklistApi = checklistApi;
        return this;
    }
}
