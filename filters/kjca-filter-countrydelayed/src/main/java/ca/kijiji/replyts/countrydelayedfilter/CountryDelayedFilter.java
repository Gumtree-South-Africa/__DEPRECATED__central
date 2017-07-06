package ca.kijiji.replyts.countrydelayedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

import static ca.kijiji.replyts.BoxHeaders.SENDER_IP_ADDRESS;

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
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();
        String ipAddress = context.getMail().getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());

        if (StringUtils.isBlank(ipAddress)) {
            LOG.debug("IP Address is empty -- not scoring");
            return feedbacks.build();
        }

        boolean countryIsDelayed = false;
        try {
            countryIsDelayed = checkIfCountryDelayedInLeGrid(ipAddress);
        } catch (Exception e) {
            LOG.warn("Exception caught when calling grid. Assuming country not delayed.", e);
        }


        if (countryIsDelayed) {
            feedbacks.add(new FilterFeedback("country is delayed", "IP country is delayed",
                    countryDelayedScore, FilterResultState.HELD));
        }

        return feedbacks.build();
    }

    private boolean checkIfCountryDelayedInLeGrid(String ipAddress) {
        Map<String, Boolean> result = this.tnsApiClient.getJsonAsMap("/replier/ip-address/" + ipAddress + "/is-country-delayed");
        boolean isDelayed = result.get(IS_COUNTRY_DELAYED_KEY);
        LOG.debug("Is {} country delayed? {}", ipAddress, isDelayed);
        return isDelayed;
    }

}
