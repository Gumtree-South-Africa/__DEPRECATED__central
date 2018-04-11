package com.ecg.comaas.gtau.filter.echelon;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

public class EchelonFilterPatternRulesParser {
    private static final int DEFAULT_SCORE = 0;

    private String endpointUrl;
    private int endpointTimeout;
    private int score;

    private EchelonFilterPatternRulesParser(JsonNode jsonNode) {
        JsonNode endpointUrlNode = jsonNode.get("endpointUrl");
        Preconditions.checkArgument(isValidString(endpointUrlNode), "given config does not contain a valid endpointUrl element.");
        endpointUrl = endpointUrlNode.textValue();
        JsonNode endpointTimeoutNode = jsonNode.get("endpointTimeout");
        Preconditions.checkArgument(isValidInt(endpointTimeoutNode), "given config does not contain a valid endpointTimeout element.");
        endpointTimeout = endpointTimeoutNode.asInt();
        JsonNode scoreNode = jsonNode.get("score");
        score = scoreNode != null ? scoreNode.asInt() : DEFAULT_SCORE;
    }

    public static boolean isValidString(JsonNode jsonNode) {
        return jsonNode != null && jsonNode.textValue() != null && !jsonNode.textValue().isEmpty();
    }

    public static boolean isValidInt(JsonNode jsonNode) {
        return jsonNode != null && jsonNode.canConvertToInt();
    }

    public static EchelonFilterConfiguration getConfig(JsonNode config) {
        EchelonFilterPatternRulesParser parser = new EchelonFilterPatternRulesParser(config);
        return new EchelonFilterConfiguration(parser.endpointUrl, parser.endpointTimeout, parser.score);
    }
}
