package com.ebay.ecg.bolt.replyts.identicalcapfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

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
        int charScanLength = getCharScanLength(rules);
        int lookupInterval = getLookupInterval(rules);
        int score = getScore(rules);
        int matchCount = getMatchCount(rules);

        String lookupIntervalTimeUnit = getLookupIntervalTimeUnit(rules);

        List<Integer> exceptCategoriesList = getExceptCategories(runFor);
        List<Integer> categoriesList = getCategories(runFor);

        return new FilterConfig(charScanLength, lookupInterval, lookupIntervalTimeUnit, score, matchCount, exceptCategoriesList, categoriesList);
    }

    private static int getCharScanLength(JsonNode n) {
        int charScanLength = n.get("charScanLength").asInt();

        Preconditions.checkArgument(charScanLength > 0, "charScanLength must be greater than 0");

        return charScanLength;
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
        List<Integer> result = new ArrayList<>();

        JsonNode arrNode = runFor.get("categories");

        if (arrNode.isArray()) {
            for (JsonNode objNode : arrNode) {
                result.add(objNode.asInt());
            }
        }

        return result;
    }

    private static List<Integer> getExceptCategories(JsonNode runFor) {
        List<Integer> result = new ArrayList<>();

        JsonNode arrNode = runFor.get("exceptCategories");

        if (arrNode.isArray()) {
            for (JsonNode objNode : arrNode) {
                result.add(objNode.asInt());
            }
        }

        return result;
    }
}