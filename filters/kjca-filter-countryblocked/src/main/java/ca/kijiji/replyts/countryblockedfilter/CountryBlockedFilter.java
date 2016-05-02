package ca.kijiji.replyts.countryblockedfilter;

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

public class CountryBlockedFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(CountryBlockedFilter.class);

    public static final String IS_COUNTRY_BLOCKED_KEY = "is-country-blocked";

    private final int countryBlockedScore;
    private final LeGridClient leGridClient;

    public CountryBlockedFilter(int countryBlockedScore, LeGridClient leGridClient) {
        this.countryBlockedScore = countryBlockedScore;
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
        Map result = leGridClient.getJsonAsMap("replier/ip-address/" + ipAddress + "/is-country-blocked");
        Boolean isBlocked = (Boolean) result.get(IS_COUNTRY_BLOCKED_KEY);
        LOG.debug("Is {} country blocked? {}", ipAddress, isBlocked);
        return isBlocked;
    }

}
