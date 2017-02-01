package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.UserService;
import de.mobile.ebay.service.impl.Config;
import de.mobile.ebay.service.impl.UserServiceImpl;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * User: acharton
 * Date: 12/18/12
 */
@Component
class UserStateFilterFactory implements FilterFactory {

    private final UserService userService;

    @Autowired
    public UserStateFilterFactory(HttpClient client, @Qualifier("esconfig-user") Config config) {
        this.userService = new UserServiceImpl(client, config);
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new UserStateFilter(s, new UserStateConfigParser(jsonNode).parse(), userService);
    }
}
