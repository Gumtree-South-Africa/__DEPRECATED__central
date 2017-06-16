package ca.kijiji.replyts.countryblockedfilter;

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
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();
        String ipAddress = context.getMail().getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());

        if (StringUtils.isBlank(ipAddress)) {
            LOG.debug("IP Address is empty -- not scoring");
            return feedbacks.build();
        }

        Boolean countryIsBlocked = false;
        try {
            countryIsBlocked = checkIfCountryBlockedInLeGrid(ipAddress);
        } catch (Exception e) {
            LOG.warn("Exception caught when calling grid. Assuming country not blocked.", e);
        }


        if (countryIsBlocked) {
            feedbacks.add(new FilterFeedback("country is blocked", "IP country is blocked",
                    countryBlockedScore, FilterResultState.DROPPED));
        }

        return feedbacks.build();
    }

    private Boolean checkIfCountryBlockedInLeGrid(String ipAddress) {
        Map result = this.tnsApiClient.getJsonAsMap("/replier/ip-address/" + ipAddress + "/is-country-blocked");
        Boolean isBlocked = (Boolean) result.get(IS_COUNTRY_BLOCKED_KEY);
        LOG.debug("Is {} country blocked? {}", ipAddress, isBlocked);
        return isBlocked;
    }

}
