package com.ecg.comaas.gtau.filter.ebayservices.userstate;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.OAuthTokenProvider;
import de.mobile.ebay.service.UserService;
import de.mobile.ebay.service.impl.Config;
import de.mobile.ebay.service.impl.UserServiceImpl;
import org.apache.http.client.HttpClient;

public class UserStateFilterFactory implements FilterFactory {

    private static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate.UserStateFilterFactory";

    private final UserService userService;

    public UserStateFilterFactory(HttpClient client, Config config, OAuthTokenProvider tokenService) {
        this.userService = new UserServiceImpl(client, config, tokenService);
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new UserStateFilter(new UserStateConfigParser(jsonNode).parse(), userService);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
