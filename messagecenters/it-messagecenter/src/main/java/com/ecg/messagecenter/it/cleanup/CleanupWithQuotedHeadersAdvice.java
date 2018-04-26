package com.ecg.messagecenter.it.cleanup;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by pragone on 19/04/15.
 */
public class CleanupWithQuotedHeadersAdvice extends AbstractCleanupAdvice {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\s*[a-zA-Z-]+:.+$");
    private static final List<String> KNOWN_HEADERS =
                    Arrays.asList("subject", "date", "sent", "to", "from", "da", "oggetto", "data",
                                    "inviato");

    protected CleanupWithQuotedHeadersAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        //        if (text.getFirstQuoteIndex() >= 0) {
        //            return;
        //        }

        for (Text.Line line : text.lines) {
            if (text.getAdvice().isLineCleaned(line.originalIndex) || text.getAdvice()
                            .isLineQuoted(line.originalIndex)) {
                continue; // Ignore already cleaned lights
            }
            if (HEADER_PATTERN.matcher(line.content).matches()) {
                String[] parts = line.content.split(":", 2);
                if (KNOWN_HEADERS.contains(parts[0].toLowerCase())) {
                    markQuoted(line.originalIndex);
                }
            }
        }

        if (beginWithUsefulLines()) {
            return;
        }

        if (foundAtLeastTwoConsecutiveHeaders()) {
            markAllQuotedFromFirstQuotedLine();
        }
    }

    private boolean isEmptyLine(Text.Line line) {
        return line.content == null || line.content.isEmpty();
    }

    private boolean beginWithUsefulLines() {
        for (Text.Line line : text.lines) {
            if (isEmptyLine(line)) {
                continue;
            }

            return this.quoted[line.originalIndex];
        }

        return false;
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

    private boolean foundAtLeastTwoConsecutiveHeaders() {
        int consecutiveQuotes = 0;
        for (Text.Line line : text.lines) {
            if (this.quoted[line.originalIndex]) {
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
