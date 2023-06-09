package com.ecg.comaas.kjca.filter.emailblocked;

import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MVCA;

@ComaasPlugin
@Profile({TENANT_KJCA, TENANT_MVCA})
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
