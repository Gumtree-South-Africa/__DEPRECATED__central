package com.ecg.comaas.core.filter.user;

import java.util.regex.Pattern;

/**
 * User: acharton
 * Date: 12/17/12
 */
public class PatternEntry {

    private final Pattern pattern;
    private final int score;

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
