package ca.kijiji.replyts.countrydelayedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class CountryDelayedFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "ca.kijiji.replyts.countrydelayedfilter.CountryDelayedFilterFactory";

    private static final String COUNTRY_DELAYED_SCORE_JSON_KEY = "countryDelayedScore";

    private final TnsApiClient tnsApiClient;

    @Autowired
    public CountryDelayedFilterFactory(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode countryDelayedScoreJsonNode = configuration.get(COUNTRY_DELAYED_SCORE_JSON_KEY);
        if (countryDelayedScoreJsonNode == null) {
            throw new IllegalStateException(COUNTRY_DELAYED_SCORE_JSON_KEY + " missing in configuration of CountryDelayedFilter");
        }

        return new CountryDelayedFilter(countryDelayedScoreJsonNode.asInt(), this.tnsApiClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
