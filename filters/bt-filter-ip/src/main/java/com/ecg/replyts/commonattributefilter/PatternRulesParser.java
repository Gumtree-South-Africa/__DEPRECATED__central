package com.ecg.replyts.commonattributefilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class PatternRulesParser {
    public static FilterConfig parse(JsonNode jsonNode) {
        JsonNode attributeNode = jsonNode.get("attribute");

        Preconditions.checkArgument(attributeNode != null, "given config does not contain an attribute element.");

        String attribute = attributeNode.asText().toLowerCase();
        
        ArrayNode rulesArray = (ArrayNode)jsonNode.get("rules");

        Preconditions.checkArgument(rulesArray != null, "given config does not contain a rules element.");

        List<PatternEntry> patterns = new ArrayList<>();

        for (JsonNode n : rulesArray) {
            patterns.add(read(n));
        }

        return new FilterConfig(Collections.unmodifiableList(patterns), attribute);
    }

    private static PatternEntry read(JsonNode n) {
        return new PatternEntry(Pattern.compile(n.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), Integer.valueOf(n.get("score").asText()));
    }
}
