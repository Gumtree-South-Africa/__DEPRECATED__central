package com.ecg.de.ebayk.messagecenter.cleanup;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by pragone on 19/04/15.
 */
public class CleanupWithQuotedHeadersAdvice extends AbstractCleanupAdvice {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^[a-zA-Z-]+:.+$");
    private static final List<String> KNOWN_HEADERS = Arrays.asList(
            "subject", "date", "to", "from"
    );

    protected CleanupWithQuotedHeadersAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {

        for (Text.Line line : text.getLines()) {
            if (text.getAdvice().isLineCleaned(line.getOriginalIndex()) ||
                    text.getAdvice().isLineQuoted(line.getOriginalIndex())) {
                continue; // Ignore already cleaned lights
            }
            if (HEADER_PATTERN.matcher(line.getContent()).matches()) {
                String[] parts = line.getContent().split(":", 2);
                if (KNOWN_HEADERS.contains(parts[0].toLowerCase())) {
                    markQuoted(line.getOriginalIndex());
                }
            }
        }
        if (foundAtLeastTwoConsecutiveHeaders()) {
            markAllQuotedFromFirstQuotedLine();
        }
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

    private boolean foundAtLeastTwoConsecutiveHeaders() {
        int consecutiveQuotes = 0;
        for (Text.Line line : text.getLines()) {
            if (this.isLineQuoted(line.getOriginalIndex())) {
                consecutiveQuotes++;
                if (consecutiveQuotes >= 2) {
                    return true;
                }
            } else {
                consecutiveQuotes = 0;
            }
        }
        return false;
    }
}
