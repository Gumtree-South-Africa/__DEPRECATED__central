package com.ecg.comaas.core.resultinspector.threshold;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import static java.util.Collections.singletonList;

@Component
public class ScoringFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector.ScoringFilterFactory";

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return messageProcessingContext -> singletonList(new FilterFeedback("", "", jsonNode.get("score").asInt(), FilterResultState.OK));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
