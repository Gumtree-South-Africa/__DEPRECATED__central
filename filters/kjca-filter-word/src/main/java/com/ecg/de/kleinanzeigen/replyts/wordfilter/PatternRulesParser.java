package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

class PatternRulesParser {

    private static final String CATEGORY_IDS_FIELD = "categoryIds";

    private final List<PatternEntry> patterns;
    private final boolean ignoreQuotedRegularExpressions;
    private final boolean ignoreFollowUps;

    PatternRulesParser(JsonNode json) {
        JsonNode ignoreQuotedRegexps = json.get("ignoreQuotedRegexps");
        ignoreQuotedRegularExpressions = ignoreQuotedRegexps != null && ignoreQuotedRegexps.asBoolean();

        JsonNode ignoreFollowUpsConfig = json.get("ignoreFollowUps");
        ignoreFollowUps = ignoreFollowUpsConfig != null && ignoreFollowUpsConfig.asBoolean();

        ImmutableList.Builder<PatternEntry> builder = ImmutableList.builder();
        ArrayNode rulesArray = (ArrayNode) json.get("rules");
        Preconditions.checkArgument(rulesArray != null, "given config does not contain a rules element.");
        for (JsonNode n : rulesArray) {
            builder.add(read(n));
        }
        patterns = builder.build();
    }

    private PatternEntry read(JsonNode json) {
        return new PatternEntry(
                Pattern.compile(json.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                Integer.valueOf(json.get("score").asText()),
                listOfCategoryIds(json));
    }

    private List<String> listOfCategoryIds(JsonNode json) {
        Iterable<JsonNode> nodes = Optional.<Iterable<JsonNode>>ofNullable(json.get(CATEGORY_IDS_FIELD)).orElse(Collections.emptyList());
        ImmutableList.Builder<String> idList = ImmutableList.builder();
        for (JsonNode categoryId : nodes) {
            idList.add(categoryId.asText());
        }
        return idList.build();
    }

    FilterConfig getConfig() {
        return new FilterConfig(ignoreQuotedRegularExpressions, ignoreFollowUps, patterns);
    }

}
