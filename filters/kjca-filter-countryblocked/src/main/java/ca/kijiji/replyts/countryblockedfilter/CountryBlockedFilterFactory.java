package ca.kijiji.replyts.countryblockedfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class CountryBlockedFilterFactory implements FilterFactory {

    private static final String COUNTRY_BLOCKED_SCORE_JSON_KEY = "countryBlockedScore";
    private static final String GRID_API_END_POINT_KEY = "gridApiEndpoint";
    private static final String GRID_API_USER_KEY = "gridApiUser";
    private static final String GRID_API_PASSWORD_KEY = "gridApiPassword";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode countryBlockedScoreJsonNode = configuration.get(COUNTRY_BLOCKED_SCORE_JSON_KEY);
        if (countryBlockedScoreJsonNode == null) {
            throw new IllegalStateException(COUNTRY_BLOCKED_SCORE_JSON_KEY + " missing in configuration of CountryBlockedFilter");
        }
        LeGridClient client = new LeGridClient(configuration.get(GRID_API_END_POINT_KEY).asText(),
                configuration.get(GRID_API_USER_KEY).asText(),
                configuration.get(GRID_API_PASSWORD_KEY).asText());
        return new CountryBlockedFilter(countryBlockedScoreJsonNode.asInt(), client);
    }

}
