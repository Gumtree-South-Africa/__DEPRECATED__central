package ca.kijiji.replyts.ipblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class IpBlockedFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "ca.kijiji.replyts.ipblockedfilter.IpBlockedFilterFactory";

    private static final String IP_BLOCKED_SCORE_JSON_KEY = "ipBlockedScore";

    private final TnsApiClient tnsApiClient;

    @Autowired
    public IpBlockedFilterFactory(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode ipBlockedScoreJsonNode = configuration.get(IP_BLOCKED_SCORE_JSON_KEY);
        if (ipBlockedScoreJsonNode == null) {
            throw new IllegalStateException(IP_BLOCKED_SCORE_JSON_KEY + " missing in configuration of IpBlockedFilter");
        }

        return new IpBlockedFilter(ipBlockedScoreJsonNode.asInt(), this.tnsApiClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
