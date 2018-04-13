package com.ecg.comaas.bt.filter.ip;

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
