package com.ecg.comaas.bt.filter.ip;

import java.util.List;

public class FilterConfig {
    private List<PatternEntry> patterns;

    private String attribute;
    
    public FilterConfig(List<PatternEntry> patterns, String attribute) {
        this.patterns = patterns;
        this.attribute = attribute;
    }

    public List<PatternEntry> getPatterns() {
        return patterns;
    }

    public String getAttribute() {
        return attribute;
    }
}
