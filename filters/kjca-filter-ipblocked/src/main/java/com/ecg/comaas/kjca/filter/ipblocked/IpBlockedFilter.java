package com.ecg.comaas.kjca.filter.ipblocked;

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

public class IpBlockedFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(IpBlockedFilter.class);

    static final String IS_BLOCKED_KEY = "is-blocked";

    private final int ipBlockedScore;
    private final TnsApiClient tnsApiClient;

    IpBlockedFilter(int ipBlockedScore, TnsApiClient tnsApiClient) {
        this.ipBlockedScore = ipBlockedScore;
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        String ipAddress = context.getMail().get().getUniqueHeader(BoxHeaders.SENDER_IP_ADDRESS.getHeaderName());

        if (StringUtils.isBlank(ipAddress)) {
            LOG.debug("IP Address is empty -- not scoring");
        } else if (StringUtils.isNotBlank(ipAddress) && checkIfIpBlockedInLeGrid(ipAddress)) {
            return ImmutableList.of(new FilterFeedback("IP is blocked", "Replier IP is blocked", ipBlockedScore, FilterResultState.DROPPED));
        }

        return ImmutableList.of();
    }

    private boolean checkIfIpBlockedInLeGrid(String ipAddress) {
        Map<String, Boolean> result = this.tnsApiClient.getJsonAsMap("/replier/ip/" + ipAddress + "/is-blocked");
        if (result.get(IS_BLOCKED_KEY) == null) {
            LOG.warn("No proper result from TnsApi for IP address {}, assuming IP is not blocked", ipAddress);
            return false;
        } else {
            boolean isBlocked = result.get(IS_BLOCKED_KEY);
            if (isBlocked) {
                LOG.debug("IP '{}' is blocked", ipAddress);
            }
            return isBlocked;
        }
    }
}
