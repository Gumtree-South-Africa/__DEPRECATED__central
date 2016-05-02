package ca.kijiji.replyts.emailblockedfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class EmailBlockedFilterFactory implements FilterFactory {

    private static final String EMAIL_BLOCKED_SCORE_JSON_KEY = "emailBlockedScore";
    private static final String GRID_API_END_POINT_KEY = "gridApiEndpoint";
    private static final String GRID_API_USER_KEY = "gridApiUser";
    private static final String GRID_API_PASSWORD_KEY = "gridApiPassword";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode emailBlockedScoreJsonNode = configuration.get(EMAIL_BLOCKED_SCORE_JSON_KEY);
        if (emailBlockedScoreJsonNode == null) {
            throw new IllegalStateException(EMAIL_BLOCKED_SCORE_JSON_KEY + " missing in configuration of EmailBlockedFilter");
        }
        LeGridClient client = new LeGridClient(configuration.get(GRID_API_END_POINT_KEY).asText(),
                configuration.get(GRID_API_USER_KEY).asText(),
                configuration.get(GRID_API_PASSWORD_KEY).asText());
        return new EmailBlockedFilter(emailBlockedScoreJsonNode.asInt(), client);
    }

}
