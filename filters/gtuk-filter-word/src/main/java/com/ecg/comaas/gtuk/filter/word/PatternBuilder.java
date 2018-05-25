package com.ecg.comaas.gtuk.filter.word;

import com.ecg.gumtree.comaas.common.domain.Rule;
import com.ecg.gumtree.comaas.common.domain.WordFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

class PatternBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PatternBuilder.class);

    private WordFilterConfig filterConfig;

    PatternBuilder(WordFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    Map<Rule, Pattern> buildAndPrecompileFromWordFilterRules() {
        Map<Rule, Pattern> compiledPatterns = new HashMap<>();

        if (filterConfig.getRules() != null) {
            for (Rule rule : filterConfig.getRules()) {
                if (rule.getPattern() == null || rule.getPattern().trim().isEmpty()) {
                    LOG.debug("Ignoring empty rule");
                } else if (compiledPatterns.containsKey(rule)) {
                    LOG.debug("Ignoring duplicate");
                } else {
                    try {
                        compiledPatterns.put(rule, Pattern.compile(build(rule), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                    } catch (PatternSyntaxException e) {
                        LOG.debug("Ignoring bogus pattern: {}", rule.getPattern(), e);
                    }
                }
            }
        }

        return compiledPatterns;
    }

    private String build(Rule rule) {
        final StringBuilder builder = new StringBuilder();

        // look for negative look-behinds (i.e. don't match when exclusion is before pattern)
        final String mixedCasePattern = rule.getPattern();
        final String pattern = mixedCasePattern.toLowerCase();

        final List<String> exceptions = getLowerCaseExceptions(rule);
        final Integer patternLength = pattern.length();

        for (final String exception : exceptions) {
            final Integer exceptionLength = exception.length();
            if (exception.regionMatches(true, (exceptionLength - patternLength), pattern, 0, patternLength)) {
                builder.append("(?<!\\b");
                builder.append(Pattern.quote(exception.replace(pattern, "").trim()));
                builder.append("\\b )");
            }
        }

        if (rule.getWordBoundaries()) {
            builder.append("\\b");
            builder.append(Pattern.quote(mixedCasePattern));
            builder.append("\\b");
        } else {
            builder.append(Pattern.quote(pattern));
        }

        // look for negative look-aheads (i.e. don't match when exclusion is after pattern)
        for (final String exception : exceptions) {
            if (exception.regionMatches(true, 0, pattern, 0, patternLength)) {
                builder.append("(?! \\b");
                builder.append(Pattern.quote(exception.replace(pattern, "").trim()));
                builder.append("\\b)");
            }
        }
        return builder.toString();
    }

    private List<String> getLowerCaseExceptions(final Rule rule) {
        List<String> exceptions = rule.getExceptions();
        if (exceptions == null) {
            return Collections.emptyList();
        }
        return exceptions.stream().map(String::toLowerCase).collect(Collectors.toList());
    }
}