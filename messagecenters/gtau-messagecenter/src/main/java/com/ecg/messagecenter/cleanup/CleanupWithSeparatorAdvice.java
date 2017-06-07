package com.ecg.messagecenter.cleanup;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CleanupWithSeparatorAdvice extends AbstractCleanupAdvice {
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^[-*]+[\\s*]+(.*)");
    private static final List<String> KNOWN_SEPARATORS = Arrays.asList(
            "original message", "forwarded message", "reply message", "messaggio originale"
    );

    protected CleanupWithSeparatorAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        for (Text.Line line : text.lines) {
            if (text.getAdvice().isLineCleaned(line.originalIndex) ||
                    text.getAdvice().isLineQuoted(line.originalIndex)) {
                continue; // Ignore already cleaned lights
            }
            Matcher matcher = SEPARATOR_PATTERN.matcher(line.content);
            if (matcher.matches()) {
                String separator = matcher.group(1);
                for(String ks : KNOWN_SEPARATORS) {
                    if(separator.toLowerCase().startsWith(ks)) {
                        markQuoted(line.originalIndex);
                    }
                }
            }
        }
        markAllQuotedFromFirstQuotedLine();
    }

    private void markAllQuotedFromFirstQuotedLine() {
        boolean finding = true;
        for (Text.Line line : text.lines) {
            if (finding) {
                if (this.quoted[line.originalIndex]) {
                    finding = false;
                }
            } else {
                this.quoted[line.originalIndex] = true;
            }
        }
    }
}