package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

class PatternRulesParser {

    private final List<PatternEntry> patterns;
    private final boolean ignoreQuotedRegularExpresssions;

    public PatternRulesParser(JsonNode jsonNode) {
        JsonNode ignoreQuotedRegexps = jsonNode.get("ignoreQuotedRegexps");
        ignoreQuotedRegularExpresssions = ignoreQuotedRegexps != null && ignoreQuotedRegexps.asBoolean();
        ImmutableList.Builder<PatternEntry> builder = ImmutableList.<PatternEntry>builder();
        ArrayNode rulesArray = (ArrayNode)jsonNode.get("rules");
        Preconditions.checkArgument(rulesArray!=null, "given config does not contain a rules element.");
        for(JsonNode n : rulesArray) {
            builder.add(read(n));
        }
        patterns = builder.build();
    }

    private PatternEntry read(JsonNode n) {
        if(n.has("categoryIds")) {
            return new PatternEntry(
                    Pattern.compile(n.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                    Integer.valueOf(n.get("score").asText()),
                    Optional.<List>of(listOfCategoryIds(n)));
        } else {
            return new PatternEntry(
                    Pattern.compile(n.get("regexp").asText(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
                    Integer.valueOf(n.get("score").asText()),
                    Optional.<List>absent());
        }
    }

    private ImmutableList<Object> listOfCategoryIds(JsonNode n) {
        Iterator<JsonNode> categoryIds = n.get("categoryIds").elements();
        ImmutableList.Builder<Object> idBuilder = ImmutableList.builder();
        do {
            JsonNode categoryId = categoryIds.next();
            idBuilder.add(categoryId.asText());
        } while (categoryIds.hasNext());
        return idBuilder.build();
    }

    public FilterConfig getConfig() {
        return new FilterConfig(ignoreQuotedRegularExpresssions, patterns);
    }

}
