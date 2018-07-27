package com.ecg.comaas.core.filter.user;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.*;

@ComaasPlugin
@Profile({TENANT_GTAU, TENANT_EBAYK, TENANT_KJCA, TENANT_MVCA})
@Component
public class UserfilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.userfilter.UserfilterFactory";

    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        return new Userfilter(new UserfilterPatternRulesParser(jsonNode).getPatterns());
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
