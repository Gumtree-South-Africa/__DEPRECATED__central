package com.ecg.comaas.kjca.filter.countryblocked;

import com.ecg.comaas.kjca.coremod.shared.BoxHeaders;
import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CountryBlockedFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(CountryBlockedFilter.class);

    static final String IS_COUNTRY_BLOCKED_KEY = "is-country-blocked";

    private final int countryBlockedScore;
    private final TnsApiClient tnsApiClient;

    CountryBlockedFilter(int countryBlockedScore, TnsApiClient tnsApiClient) {
        this.countryBlockedScore = countryBlockedScore;
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        String ipAddress = context.getMail().get().getUniqueHeader(BoxHeaders.SENDER_IP_ADDRESS.getHeaderName());

        if (StringUtils.isBlank(ipAddress)) {
            LOG.trace("IP Address is empty -- not scoring");
        } else if (StringUtils.isNotBlank(ipAddress) && checkIfCountryBlockedInLeGrid(ipAddress)) {
            return ImmutableList.of(new FilterFeedback("country is blocked", "IP country is blocked", countryBlockedScore, FilterResultState.DROPPED));
        }

        return ImmutableList.of();
    }

    private boolean checkIfCountryBlockedInLeGrid(String ipAddress) {
        Map<String, Boolean> result = this.tnsApiClient.getJsonAsMap("/replier/ip-address/" + ipAddress + "/is-country-blocked");
        if (result.get(IS_COUNTRY_BLOCKED_KEY) == null) {
            LOG.warn("No proper result from TnsApi for IP address {}, assuming country is not blocked", ipAddress);
            return false;
        } else {
            boolean isBlocked = result.get(IS_COUNTRY_BLOCKED_KEY);
            if (isBlocked) {
                LOG.debug("Country {} is blocked", ipAddress);
            }
            return isBlocked;
        }
    }
}
