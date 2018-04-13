package com.ecg.comaas.bt.filter.word;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class WordfilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.wordfilter.WordfilterFactory";

    @Override
    public Filter createPlugin(String filterName, JsonNode jsonNode) {
        return new Wordfilter(PatternRulesParser.parse(jsonNode));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
