package com.gumtree.comaas.filter.geoip;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.GeoIpFilterConfig;
import com.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.*;

public class GumtreeGeoIpFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeGeoIpFilter.class);

    private static final Timer TIMER = TimingReports.newTimer("geoip-filter-process-time");

    private Filter pluginConfig;
    private GeoIpFilterConfig filterConfig;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        try (Timer.Context ignore = TIMER.time()) {
            if (!context.getMessage().getMessageDirection().equals(MessageDirection.BUYER_TO_SELLER)) {
                return Collections.emptyList();
            }

            Set<Long> categoryBreadCrumb = (Set<Long>) context.getFilterContext().get("categoryBreadCrumb");

            if (hasExemptedCategory(filterConfig.getExemptedCategories(), categoryBreadCrumb)) {
                return Collections.emptyList();
            }

            String country = (String) context.getFilterContext().get("country");

            if (country == null) {
                LOG.debug("Could not determine country of mail");
                // location could not be resolved. return no/empty result without assigning a score.
                return Collections.emptyList();
            }

            LOG.debug("Origin is {}", country);

            if (filterConfig.getCountrySet().contains(country)) {
                String ip = context.getMessage().getHeaders().get(GumtreeCustomHeaders.BUYER_IP.getHeaderValue());
                String shortDescription = "Filtered country " + country;
                String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), shortDescription);

                FilterFeedback reason = new FilterFeedback(ip, description, 0, resultFilterResultMap.get(filterConfig.getResult()));
                return Collections.singletonList(reason);
            }

            return Collections.emptyList();
        }
    }

    GumtreeGeoIpFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeGeoIpFilter withFilterConfig(GeoIpFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }
}
