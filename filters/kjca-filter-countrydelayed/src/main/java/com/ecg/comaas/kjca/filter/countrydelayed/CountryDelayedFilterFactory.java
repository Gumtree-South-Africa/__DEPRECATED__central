package com.ecg.comaas.kjca.filter.countrydelayed;

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
public class CountryDelayedFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "ca.kijiji.replyts.countrydelayedfilter.CountryDelayedFilterFactory";

    private static final String COUNTRY_DELAYED_SCORE_JSON_KEY = "countryDelayedScore";

    private final TnsApiClient tnsApiClient;

    @Autowired
    public CountryDelayedFilterFactory(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode countryDelayedScoreJsonNode = configuration.get(COUNTRY_DELAYED_SCORE_JSON_KEY);
        if (countryDelayedScoreJsonNode == null) {
            throw new IllegalStateException(COUNTRY_DELAYED_SCORE_JSON_KEY + " missing in configuration of CountryDelayedFilter");
        }

        return new CountryDelayedFilter(countryDelayedScoreJsonNode.asInt(), this.tnsApiClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
