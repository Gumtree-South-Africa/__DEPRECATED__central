package com.ecg.comaas.core.filter.ebayservices.userstate;

import com.ecg.comaas.core.filter.ebayservices.userstate.provider.UserStateProvider;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class UserStateFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate.UserStateFilterFactory";

    private final UserStateProvider userStateProvider;

    public UserStateFilterFactory(UserStateProvider userStateProvider) {
        this.userStateProvider = userStateProvider;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode jsonNode) {
        return new UserStateFilter(new UserStateFilterConfigHolder(jsonNode), userStateProvider);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
