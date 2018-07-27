package com.ecg.comaas.kjca.filter.ipblocked;

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
