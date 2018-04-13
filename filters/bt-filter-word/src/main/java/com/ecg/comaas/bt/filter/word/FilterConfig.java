package com.ecg.comaas.bt.filter.word;

import java.util.List;

public class FilterConfig {
    private boolean ignoreQuotedPatterns;

    private List<PatternEntry> patterns;

    public FilterConfig(boolean ignoreQuotedPatterns, List<PatternEntry> patterns) {
        this.ignoreQuotedPatterns = ignoreQuotedPatterns;
        this.patterns = patterns;
    }

    public boolean isIgnoreQuotedPatterns() {
        return ignoreQuotedPatterns;
    }

    public List<PatternEntry> getPatterns() {
        return patterns;
    }
}
