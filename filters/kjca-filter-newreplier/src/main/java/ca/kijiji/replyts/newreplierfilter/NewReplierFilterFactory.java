package ca.kijiji.replyts.newreplierfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class NewReplierFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "ca.kijiji.replyts.newreplierfilter.NewReplierFilterFactory";

    private static final String ISNEW_SCORE_JSON_KEY = "isNewScore";

    private final TnsApiClient tnsApiClient;

    @Autowired
    public NewReplierFilterFactory(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode isNewScoreJsonNode = configuration.get(ISNEW_SCORE_JSON_KEY);
        if (isNewScoreJsonNode == null) {
            throw new IllegalStateException(ISNEW_SCORE_JSON_KEY + " missing in configuration of NewReplierFilter");
        }

        return new NewReplierFilter(isNewScoreJsonNode.asInt(), new Activation(configuration), this.tnsApiClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
