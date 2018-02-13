package com.ebay.ecg.bolt.replyts.identicalcapfilter;

import java.util.List;

public class FilterConfig {
    private int charScanLength;

    private int lookupInterval;

    private String lookupIntervalTimeUnit;

    private int score;

    private int matchCount;

    private List<Integer> exceptCategories;

    private List<Integer> categories;
 
    public FilterConfig(int charScanLength, int lookupInterval, String lookupIntervalTimeUnit, int score, int matchCount, List<Integer> exceptCategories, List<Integer> categories) {
        this.charScanLength = charScanLength;
        this.lookupInterval = lookupInterval;
        this.lookupIntervalTimeUnit = lookupIntervalTimeUnit;
        this.score = score;
        this.matchCount = matchCount;
        this.exceptCategories= exceptCategories;
        this.categories = categories;
    }

    public int getCharScanLength() {
        return charScanLength;
    }

    public int getLookupInterval() {
        return lookupInterval;
    }

    public String getLookupIntervalTimeUnit() {
        return lookupIntervalTimeUnit;
    }

    public int getScore() {
        return score;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public List<Integer> getExceptCategories() {
        return exceptCategories;
    }

    public List<Integer> getCategories() {
        return categories;
    }
}