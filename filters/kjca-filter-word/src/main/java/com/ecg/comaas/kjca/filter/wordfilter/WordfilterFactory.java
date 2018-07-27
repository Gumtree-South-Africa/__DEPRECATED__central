package com.ecg.comaas.kjca.filter.wordfilter;

import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MVCA;

@ComaasPlugin
@Profile({TENANT_KJCA, TENANT_MVCA})
@Component
public class WordfilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.wordfilter.WordfilterFactory";

    private long regexProcessingTimeoutMs;

    @Autowired
    public WordfilterFactory(@Value("${replyts2-wordfilter-plugin.regex.processingTimeoutMs:600000}") long regexProcessingTimeoutMs) {
        this.regexProcessingTimeoutMs = regexProcessingTimeoutMs;
    }

    @Override
    public Filter createPlugin(String filtername, JsonNode jsonNode) {
        PatternRulesParser ruleParser = new PatternRulesParser(jsonNode);
        Activation activation = new Activation(jsonNode);
        return new Wordfilter(ruleParser.getConfig(), activation, regexProcessingTimeoutMs);
    }

    @Override
        public String getIdentifier() {
        return IDENTIFIER;
    }
}
