package com.ecg.comaas.kjca.filter.newreplier;

import com.ecg.comaas.core.filter.activable.Activation;
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
