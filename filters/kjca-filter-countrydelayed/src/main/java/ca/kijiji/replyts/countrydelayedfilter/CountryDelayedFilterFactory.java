package ca.kijiji.replyts.countrydelayedfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class CountryDelayedFilterFactory implements FilterFactory {

    private static final String COUNTRY_DELAYED_SCORE_JSON_KEY = "countryDelayedScore";
    private static final String GRID_API_END_POINT_KEY = "gridApiEndpoint";
    private static final String GRID_API_USER_KEY = "gridApiUser";
    private static final String GRID_API_PASSWORD_KEY = "gridApiPassword";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode countryDelayedScoreJsonNode = configuration.get(COUNTRY_DELAYED_SCORE_JSON_KEY);
        if (countryDelayedScoreJsonNode == null) {
            throw new IllegalStateException(COUNTRY_DELAYED_SCORE_JSON_KEY + " missing in configuration of CountryDelayedFilter");
        }
        LeGridClient client = new LeGridClient(configuration.get(GRID_API_END_POINT_KEY).asText(),
                configuration.get(GRID_API_USER_KEY).asText(),
                configuration.get(GRID_API_PASSWORD_KEY).asText());
        return new CountryDelayedFilter(countryDelayedScoreJsonNode.asInt(), client);
    }

}
