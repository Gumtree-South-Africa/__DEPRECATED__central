package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import java.util.List;

class FilterConfig {

    private final boolean ignoreQuotedPatterns;
    private final boolean ignoreFollowUps;
    private final List<PatternEntry> patterns;

    FilterConfig(boolean ignoreQuotedPatterns, boolean ignoreFollowUps, List<PatternEntry> patterns) {
        this.ignoreQuotedPatterns = ignoreQuotedPatterns;
        this.ignoreFollowUps = ignoreFollowUps;
        this.patterns = patterns;
    }

    boolean isIgnoreQuotedPatterns() {
        return ignoreQuotedPatterns;
    }

    boolean isIgnoreFollowUps() {
        return ignoreFollowUps;
    }

    List<PatternEntry> getPatterns() {
        return patterns;
    }
}
