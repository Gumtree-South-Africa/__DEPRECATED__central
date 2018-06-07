package com.ecg.comaas.kjca.filter.countrydelayed;

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

public class CountryDelayedFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(CountryDelayedFilter.class);

    static final String IS_COUNTRY_DELAYED_KEY = "is-country-delayed";

    private final int countryDelayedScore;
    private final TnsApiClient tnsApiClient;

    CountryDelayedFilter(int countryDelayedScore, TnsApiClient tnsApiClient) {
        this.countryDelayedScore = countryDelayedScore;
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        String ipAddress = context.getMail().get().getUniqueHeader(BoxHeaders.SENDER_IP_ADDRESS.getHeaderName());

        if (StringUtils.isBlank(ipAddress)) {
            LOG.trace("IP Address is empty -- not scoring");
        } else if (StringUtils.isNotBlank(ipAddress) && checkIfCountryDelayedInLeGrid(ipAddress)) {
            return ImmutableList.of(new FilterFeedback("country is delayed", "IP country is delayed", countryDelayedScore, FilterResultState.HELD));
        }

        return ImmutableList.of();
    }

    private boolean checkIfCountryDelayedInLeGrid(String ipAddress) {
        Map<String, Boolean> result = this.tnsApiClient.getJsonAsMap("/replier/ip-address/" + ipAddress + "/is-country-delayed");
        if (result.get(IS_COUNTRY_DELAYED_KEY) == null) {
            LOG.warn("No proper result from TnsApi for IP address {}, assuming country is not delayed", ipAddress);
            return false;
        } else {
            boolean isDelayed = result.get(IS_COUNTRY_DELAYED_KEY);
            if (isDelayed) {
                LOG.debug("Country '{}' is delayed", ipAddress);
            }
            return isDelayed;
        }
    }
}
