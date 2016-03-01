package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

class PatternEntry {

    private final Pattern pattern;
    private final int score;
    private final List<String> categoryIds;

    PatternEntry(Pattern pattern, int score, List<String> categoryIds) {

        checkNotNull(pattern);
        checkNotNull(categoryIds);

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

    public List<String> getCategoryIds() {
        return categoryIds;
    }
}
