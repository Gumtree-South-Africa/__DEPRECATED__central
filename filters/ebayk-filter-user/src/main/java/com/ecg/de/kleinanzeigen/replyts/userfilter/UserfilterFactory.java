package com.ecg.de.kleinanzeigen.replyts.userfilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class UserfilterFactory implements FilterFactory {

    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        return new Userfilter(new UserfilterPatternRulesParser(jsonNode).getPatterns());
    }
}
