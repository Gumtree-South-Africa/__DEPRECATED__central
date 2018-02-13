package com.ebay.ecg.bolt.replyts.dedupefilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ConfigurationParser {
    private static final Set<TimeUnit> ALLOWED_TIME_UNITS = Sets.immutableEnumSet(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);

    public static FilterConfig parse(JsonNode jsonNode) {
        JsonNode rules = jsonNode.get("rules");

        Preconditions.checkArgument(rules != null, "given config does not contain a rules element.");

        JsonNode runFor = jsonNode.get("runFor");

        Preconditions.checkArgument(runFor != null, "given config does not contain a runFor element.");

        return convert(rules, runFor);
    }

    private static FilterConfig convert(JsonNode rules, JsonNode runFor) {
        String minShouldMatch = getMinShouldMatch(rules);
        int lookupInterval = getLookupInterval(rules);
        String lookupIntervalTimeUnit = getLookupIntervalTimeUnit(rules);
        int score = getScore(rules);
        int matchCount = getMatchCount(rules);

        List<Integer> exceptCategoriesList =  getExceptCategories(runFor);
        List<Integer> categoriesList  = getCategories(runFor);

        return new FilterConfig(minShouldMatch, lookupInterval, lookupIntervalTimeUnit, score, matchCount, exceptCategoriesList, categoriesList);
    }

    private static String getMinShouldMatch(JsonNode n) {
        String minimumShouldMatch = n.get("minimumShouldMatch").asText();

        Preconditions.checkNotNull(minimumShouldMatch, "minimumShouldMatch should not be null");

        return minimumShouldMatch;
    }

    private static int getLookupInterval(JsonNode n) {
        int lookupInterval = n.get("lookupInterval").asInt();

        Preconditions.checkArgument(lookupInterval > 0, "lookupInterval must be greater than 0");

        return lookupInterval;
    }

    private static String getLookupIntervalTimeUnit(JsonNode n) {
        TimeUnit lookupIntervalTimeUnit = TimeUnit.valueOf(n.get("lookupIntervalTimeUnit").asText());

        Preconditions.checkArgument(ALLOWED_TIME_UNITS.contains(lookupIntervalTimeUnit), "lookupIntervalTimeUnit can only be SECONDS, MINUTES, HOURS or DAYS");

        return lookupIntervalTimeUnit.name();
    }

    private static int getScore(JsonNode n) {
        int score = n.get("score").asInt();

        Preconditions.checkArgument(score > 0, "score must be greater than 0");

        return score;
    }

    private static int getMatchCount(JsonNode n) {
        int matchCount = n.get("matchCount").asInt();

        Preconditions.checkArgument(matchCount > 0, "matchCount must be greater than 0");

        return matchCount;
    }

    private static List<Integer> getCategories(JsonNode runFor) {
        List<Integer> catList = new ArrayList<>();

        JsonNode arrNode = runFor.get("categories");

        if (arrNode.isArray()){
            for (JsonNode objNode : arrNode ) {
                catList.add(objNode.asInt());
            }
        }

        return catList;
    }

    private static List<Integer> getExceptCategories(JsonNode runFor) {
        List<Integer> exceptCatList = new ArrayList<>();

        JsonNode arrNode = runFor.get("exceptCategories");

        if (arrNode.isArray()){
            for (JsonNode objNode : arrNode) {
                exceptCatList.add(objNode.asInt());
            }
        }

        return exceptCatList;
    }
}