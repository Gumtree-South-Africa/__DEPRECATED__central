package ca.kijiji.replyts.ipblockedfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class IpBlockedFilterFactory implements FilterFactory {

    private static final String IP_BLOCKED_SCORE_JSON_KEY = "ipBlockedScore";
    private static final String GRID_API_END_POINT_KEY = "gridApiEndpoint";
    private static final String GRID_API_USER_KEY = "gridApiUser";
    private static final String GRID_API_PASSWORD_KEY = "gridApiPassword";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode ipBlockedScoreJsonNode = configuration.get(IP_BLOCKED_SCORE_JSON_KEY);
        if (ipBlockedScoreJsonNode == null) {
            throw new IllegalStateException(IP_BLOCKED_SCORE_JSON_KEY + " missing in configuration of IpBlockedFilter");
        }
        LeGridClient client = new LeGridClient(configuration.get(GRID_API_END_POINT_KEY).asText(),
                configuration.get(GRID_API_USER_KEY).asText(),
                configuration.get(GRID_API_PASSWORD_KEY).asText());
        return new IpBlockedFilter(ipBlockedScoreJsonNode.asInt(), client);
    }

}
