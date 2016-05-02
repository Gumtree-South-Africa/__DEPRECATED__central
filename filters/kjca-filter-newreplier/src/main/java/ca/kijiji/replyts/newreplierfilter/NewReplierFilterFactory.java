package ca.kijiji.replyts.newreplierfilter;

import ca.kijiji.replyts.Activation;
import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class NewReplierFilterFactory implements FilterFactory {

    private static final String ISNEW_SCORE_JSON_KEY = "isNewScore";
    private static final String GRID_API_END_POINT_KEY = "gridApiEndpoint";
    private static final String GRID_API_USER_KEY = "gridApiUser";
    private static final String GRID_API_PASSWORD_KEY = "gridApiPassword";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode isNewScoreJsonNode = configuration.get(ISNEW_SCORE_JSON_KEY);
        if (isNewScoreJsonNode == null) {
            throw new IllegalStateException(ISNEW_SCORE_JSON_KEY + " missing in configuration of NewReplierFilter");
        }
        LeGridClient client = new LeGridClient(configuration.get(GRID_API_END_POINT_KEY).asText(),
                configuration.get(GRID_API_USER_KEY).asText(),
                configuration.get(GRID_API_PASSWORD_KEY).asText());

        return new NewReplierFilter(isNewScoreJsonNode.asInt(), client, new Activation(configuration));
    }
}
