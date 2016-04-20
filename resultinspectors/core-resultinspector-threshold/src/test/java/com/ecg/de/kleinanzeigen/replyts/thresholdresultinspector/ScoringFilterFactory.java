package com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static java.util.Collections.singletonList;

public class ScoringFilterFactory implements FilterFactory {
    public Filter createPlugin(String s, JsonNode jsonNode) {
        final int score = jsonNode.get("score").asInt();
        return new Filter() {
            public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
                return singletonList(new FilterFeedback("", "", score, FilterResultState.OK));
            }
        };
    }
}
