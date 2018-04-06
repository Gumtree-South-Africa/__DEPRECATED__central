package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * User: acharton
 * Date: 12/18/12
 */
@Component
class UserStateFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate.UserStateFilterFactory";

    private final UserProfileService userService;

    @Autowired
    public UserStateFilterFactory(@Qualifier("ebayUserProfileService") UserProfileService userProfileService) {
        this.userService = userProfileService;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode jsonNode) {
        return new UserStateFilter(new UserStateConfigParser(jsonNode).parse(), userService);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
