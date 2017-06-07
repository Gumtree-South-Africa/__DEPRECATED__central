package com.ecg.messagecenter.cleanup;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CleanupWithSeparatorAdvice extends AbstractCleanupAdvice {
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^[-*]+[\\s]+(.*)[\\s]+[-*]+$");
    private static final List<String> KNOWN_SEPARATORS = Arrays.asList("original message", "forwarded message");

    protected CleanupWithSeparatorAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        for (Text.Line line : text.getLines()) {
            if (text.getAdvice().isLineCleaned(line.getOriginalIndex()) ||
                    text.getAdvice().isLineQuoted(line.getOriginalIndex())) {
                continue; // Ignore already cleaned lights
            }
            Matcher matcher = SEPARATOR_PATTERN.matcher(line.getContent());
            if (matcher.matches()) {
                String separator = matcher.group(1);
                if (KNOWN_SEPARATORS.contains(separator.toLowerCase())) {
                    markQuoted(line.getOriginalIndex());
                }
            }
        }
        markAllQuotedFromFirstQuotedLine();
    }

    private void markAllQuotedFromFirstQuotedLine() {
        boolean finding = true;
        for (Text.Line line : text.getLines()) {
            if (finding) {
                if (this.isLineQuoted(line.getOriginalIndex())) {
                    finding = false;
                }
            } else {
                this.setQuoted(line.getOriginalIndex(), true);
            }
        }
    }
}