package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class PatternEntry {
    private Pattern pattern;

    private int score;

    private Optional<List<String>> categoryIds;

    public PatternEntry(Pattern pattern, int score, Optional<List<String>> categoryIds) {
        this.pattern = pattern;
        this.score = score;
        this.categoryIds = categoryIds;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getScore() {
        return score;
    }

    public Optional<List<String>> getCategoryIds() {
        return categoryIds;
    }
}
