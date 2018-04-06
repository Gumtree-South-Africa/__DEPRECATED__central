package ca.kijiji.replyts.emailblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class EmailBlockedFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "ca.kijiji.replyts.emailblockedfilter.EmailBlockedFilterFactory";

    private static final String EMAIL_BLOCKED_SCORE_JSON_KEY = "emailBlockedScore";

    private final TnsApiClient tnsApiClient;

    @Autowired
    public EmailBlockedFilterFactory(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode emailBlockedScoreJsonNode = configuration.get(EMAIL_BLOCKED_SCORE_JSON_KEY);
        if (emailBlockedScoreJsonNode == null) {
            throw new IllegalStateException(EMAIL_BLOCKED_SCORE_JSON_KEY + " missing in configuration of EmailBlockedFilter");
        }
        return new EmailBlockedFilter(emailBlockedScoreJsonNode.asInt(), this.tnsApiClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
