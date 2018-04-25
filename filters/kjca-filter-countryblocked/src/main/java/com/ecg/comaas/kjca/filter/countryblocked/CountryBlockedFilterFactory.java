package com.ecg.comaas.kjca.filter.countryblocked;

import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;

@ComaasPlugin
@Profile(TENANT_KJCA)
@Component
public class CountryBlockedFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "ca.kijiji.replyts.countryblockedfilter.CountryBlockedFilterFactory";

    private static final String COUNTRY_BLOCKED_SCORE_JSON_KEY = "countryBlockedScore";

    private final TnsApiClient tnsApiClient;

    @Autowired
    public CountryBlockedFilterFactory(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode countryBlockedScoreJsonNode = configuration.get(COUNTRY_BLOCKED_SCORE_JSON_KEY);
        if (countryBlockedScoreJsonNode == null) {
            throw new IllegalStateException(COUNTRY_BLOCKED_SCORE_JSON_KEY + " missing in configuration of CountryBlockedFilter");
        }

        return new CountryBlockedFilter(countryBlockedScoreJsonNode.asInt(), this.tnsApiClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
