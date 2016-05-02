package ca.kijiji.replyts.ipblockedfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static ca.kijiji.replyts.BoxHeaders.SENDER_IP_ADDRESS;

public class IpBlockedFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(IpBlockedFilter.class);

    public static final String IS_BLOCKED_KEY = "is-blocked";

    private final int ipBlockedScore;
    private final LeGridClient leGridClient;

    public IpBlockedFilter(int ipBlockedScore, LeGridClient leGridClient) {
        this.ipBlockedScore = ipBlockedScore;
        this.leGridClient = leGridClient;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();
        String ipAddress = context.getMail().getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());

        if (!StringUtils.hasText(ipAddress)) {
            LOG.debug("IP Address is empty -- not scoring");
            return feedbacks.build();
        }

        Boolean ipIsBlocked = false;
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

    private Boolean checkIfIpBlockedInLeGrid(String ipAddress) {
        Map result = leGridClient.getJsonAsMap("replier/ip/" + ipAddress + "/is-blocked");
        Boolean isBlocked = (Boolean) result.get(IS_BLOCKED_KEY);
        LOG.debug("Is IP {} blocked? {}", ipAddress, isBlocked);
        return isBlocked;
    }

}
