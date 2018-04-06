package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/** Factory that generates wordfilters form a json config that basically contains an array of pattern/score pairs. */
class WordfilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.wordfilter.WordfilterFactory";

    private long regexProcessingTimeoutMs;

    @Autowired
    WordfilterFactory(@Value("${replyts2-wordfilter-plugin.regex.processingTimeoutMs:600000}") long regexProcessingTimeoutMs) {
        this.regexProcessingTimeoutMs = regexProcessingTimeoutMs;
    }

    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        PatternRulesParser ruleParser = new PatternRulesParser(jsonNode);
        return new Wordfilter(ruleParser.getConfig(), regexProcessingTimeoutMs);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
