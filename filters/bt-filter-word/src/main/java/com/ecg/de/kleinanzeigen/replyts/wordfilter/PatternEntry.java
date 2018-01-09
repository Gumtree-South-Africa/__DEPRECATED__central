package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.google.common.base.Optional;

import java.util.List;
import java.util.regex.Pattern;

class PatternEntry {
    private final Pattern pattern;
    private final int score;
    private Optional<List> categoryIds;

    public PatternEntry(Pattern pattern, int score, Optional<List> categoryIds) {
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

    public Optional<List> getCategoryIds() {
        return categoryIds;
    }
}
