package com.ecg.replyts.commonattributefilter;

import java.util.regex.Pattern;

public class PatternEntry {
    private Pattern pattern;

    private int score;

    public PatternEntry(Pattern pattern, int score) {
        this.pattern = pattern;
        this.score = score;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getScore() {
        return score;
    }
}
