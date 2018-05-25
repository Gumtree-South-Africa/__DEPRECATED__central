package com.ecg.comaas.gtuk.filter.watchlist;

import com.codahale.metrics.Timer;
import com.ecg.gumtree.comaas.common.domain.WatchlistFilterConfig;
import com.ecg.gumtree.comaas.common.filter.Filter;
import com.ecg.gumtree.comaas.common.gumshield.GumshieldClient;
import com.ecg.gumtree.comaas.common.gumshield.ApiChecklistAttribute;
import com.ecg.gumtree.comaas.common.gumshield.ApiChecklistType;
import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.*;

public class GumtreeWatchlistFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Timer TIMER = TimingReports.newTimer("watchlist-filter-process-time");

    private Filter pluginConfig;
    private WatchlistFilterConfig watchListFilterConfig;
    private GumshieldClient gumshieldClient;

    private static final String SHORT_DESCRIPTION = "Sender on watchlist";

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) {
        try (Timer.Context ignored = TIMER.time()) {
            if (hasExemptedCategory(watchListFilterConfig, messageContext)) {
                return Collections.emptyList();
            }

            List<FilterFeedback> reasons = new ArrayList<>();
            Mail mail = messageContext.getMail().get();
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
        return gumshieldClient.existsEntryByValue(ApiChecklistType.WATCH, checklistAttribute, attribute);
    }

    GumtreeWatchlistFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeWatchlistFilter withFilterConfig(WatchlistFilterConfig filterConfig) {
        this.watchListFilterConfig = filterConfig;
        return this;
    }

    GumtreeWatchlistFilter withClient(GumshieldClient gumshieldClient) {
        this.gumshieldClient = gumshieldClient;
        return this;
    }
}
