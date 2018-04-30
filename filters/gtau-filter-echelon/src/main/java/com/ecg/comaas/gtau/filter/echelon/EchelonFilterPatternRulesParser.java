package com.ecg.comaas.gtau.filter.echelon;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

public final class EchelonFilterPatternRulesParser {

    private static final int DEFAULT_SCORE = 0;

    private EchelonFilterPatternRulesParser() {
    }

    public static EchelonFilterConfiguration fromJson(JsonNode jsonNode) {
        String endpointUrl = getEndpointUrl(jsonNode);
        int endpointTimeout = getEndpointTimeout(jsonNode);
        int score = getScore(jsonNode);
        return new EchelonFilterConfiguration(endpointUrl, endpointTimeout, score);
    }

    private static String getEndpointUrl(JsonNode jsonNode) {
        JsonNode endpointUrlNode = jsonNode.get("endpointUrl");
        if (endpointUrlNode == null || StringUtils.isEmpty(endpointUrlNode.textValue())) {
            throw new IllegalArgumentException("endpointUrl is invalid - " + endpointUrlNode);
        }
        return endpointUrlNode.textValue();
    }

    private static int getEndpointTimeout(JsonNode jsonNode) {
        JsonNode endpointTimeoutNode = jsonNode.get("endpointTimeout");
        if (endpointTimeoutNode == null || !endpointTimeoutNode.canConvertToInt()) {
            throw new IllegalArgumentException("endpointTimeout is invalid - " + endpointTimeoutNode);
        }
        return endpointTimeoutNode.asInt();
    }

    private static int getScore(JsonNode jsonNode) {
        JsonNode scoreNode = jsonNode.get("score");
        if (scoreNode == null || !scoreNode.canConvertToInt()) {
            return DEFAULT_SCORE;
        }
        return scoreNode.asInt();
    }
}
