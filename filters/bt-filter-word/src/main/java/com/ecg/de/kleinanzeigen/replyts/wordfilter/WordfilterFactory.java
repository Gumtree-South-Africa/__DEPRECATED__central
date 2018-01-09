package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

/** Factory that generates wordfilters form a json config that basically contains an array of pattern/score pairs. */
class WordfilterFactory implements FilterFactory {
    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        PatternRulesParser ruleParser = new PatternRulesParser(jsonNode);
        return new Wordfilter(ruleParser.getConfig());
    }
}
