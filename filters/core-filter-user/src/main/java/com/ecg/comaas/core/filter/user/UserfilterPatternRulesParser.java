package com.ecg.comaas.core.filter.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Pattern;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class UserfilterPatternRulesParser {

    private final List<PatternEntry> patterns;

    public UserfilterPatternRulesParser(JsonNode jsonNode) {
        ImmutableList.Builder<PatternEntry> builder = ImmutableList.<PatternEntry>builder();
        ArrayNode rulesArray = (ArrayNode)jsonNode.get("rules");
        Preconditions.checkArgument(rulesArray != null, "given config does not contain a rules element.");
        for(JsonNode n : rulesArray) {
            builder.add(read(n));
        }

        patterns = builder.build();
    }

    private PatternEntry read(JsonNode n) {
        return new PatternEntry(
                Pattern.compile(n.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                Integer.valueOf(n.get("score").asText()));
    }

    public List<PatternEntry> getPatterns() {
        return patterns;
    }
}
