package ca.kijiji.replyts.ipblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static ca.kijiji.replyts.BoxHeaders.SENDER_IP_ADDRESS;

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
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();
        String ipAddress = context.getMail().getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());

        if (StringUtils.isBlank(ipAddress)) {
            LOG.debug("IP Address is empty -- not scoring");
            return feedbacks.build();
        }

        boolean ipIsBlocked = false;
        try {
            ipIsBlocked = checkIfIpBlockedInLeGrid(ipAddress);
        } catch (Exception e) {
            LOG.warn("Exception caught when calling grid. Assuming IP not blocked.", e);
        }


        if (ipIsBlocked) {
            feedbacks.add(new FilterFeedback("IP is blocked", "Replier IP is blocked",
                    ipBlockedScore, FilterResultState.DROPPED));
        }

        return feedbacks.build();
    }

    private boolean checkIfIpBlockedInLeGrid(String ipAddress) {
        Map<String, Boolean> result = this.tnsApiClient.getJsonAsMap("/replier/ip/" + ipAddress + "/is-blocked");
        boolean isBlocked = result.get(IS_BLOCKED_KEY);
        LOG.trace("Is IP {} blocked? {}", ipAddress, isBlocked);
        return isBlocked;
    }

}
