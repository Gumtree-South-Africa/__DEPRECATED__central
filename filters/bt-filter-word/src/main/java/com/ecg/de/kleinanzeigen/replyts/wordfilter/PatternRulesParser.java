package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;

import java.util.*;
import java.util.regex.Pattern;

public class PatternRulesParser {
    public static FilterConfig parse(JsonNode jsonNode) {
        JsonNode ignoreQuotedRegexps = jsonNode.get("ignoreQuotedRegexps");

        boolean ignoreQuotedRegularExpresssions = ignoreQuotedRegexps != null && ignoreQuotedRegexps.asBoolean();

        ArrayNode rulesArray = (ArrayNode) jsonNode.get("rules");

        Preconditions.checkArgument(rulesArray != null, "given config does not contain a rules element.");

        List<PatternEntry> patterns = new ArrayList<>();

        for(JsonNode n : rulesArray) {
            patterns.add(read(n));
        }

        return new FilterConfig(ignoreQuotedRegularExpresssions, patterns);
    }

    private static PatternEntry read(JsonNode n) {
        if (n.has("categoryIds")) {
            return new PatternEntry(Pattern.compile(n.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), Integer.valueOf(n.get("score").asText()),
              Optional.of(listOfCategoryIds(n)));
        } else {
            return new PatternEntry(Pattern.compile(n.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), Integer.valueOf(n.get("score").asText()),
              Optional.empty());
        }
    }

    private static List<String> listOfCategoryIds(JsonNode n) {
        Iterator<JsonNode> categoryIds = n.get("categoryIds").elements();

        List<String> result = new ArrayList<>();

        do {
            JsonNode categoryId = categoryIds.next();
            result.add(categoryId.asText());
        } while (categoryIds.hasNext());

        return Collections.unmodifiableList(result);
    }
}