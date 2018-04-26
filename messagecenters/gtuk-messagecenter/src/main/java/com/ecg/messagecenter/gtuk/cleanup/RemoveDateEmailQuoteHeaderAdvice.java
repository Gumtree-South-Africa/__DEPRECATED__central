package com.ecg.messagecenter.gtuk.cleanup;

import java.util.regex.Pattern;

public class RemoveDateEmailQuoteHeaderAdvice extends AbstractCleanupAdvice {
    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("[0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4}"),
            Pattern.compile("[0-9]{4}[/-][0-9]{1,2}[/-][0-9]{1,2}"),
            Pattern.compile("[0-9]{1,2} [a-zA-Z]{2,10} [0-9]{4}"),
            Pattern.compile("[a-zA-Z]{2,10} [0-9]{1,2}, [0-9]{4}"),
            Pattern.compile("[0-9]{4}年[0-9]{1,2}月[0-9]{1,2}日")
    };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");

    protected RemoveDateEmailQuoteHeaderAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        int index = text.getFirstQuoteIndex();
        boolean keepChecking = true;
        int removedLines = 0;
        while (index > 0 && keepChecking && removedLines < 2) {
            // There's at least a line before it
            final int prevIndex = index - 1;
            if (text.getAdvice().isLineCleaned(prevIndex) || text.getAdvice().isLineQuoted(prevIndex)) {
                index--;
                continue; // skip
            }
            final String theLine = text.getLineWithOriginalIndex(prevIndex).getContent();
            if (lineContainsADate(theLine)) {
                markQuoted(prevIndex);
                keepChecking = false;
            } else if (removedLines == 0 && lineContainsAnEmail(theLine)) {
                markQuoted(prevIndex);
                index--;
                removedLines++;
            } else {
                keepChecking = false;
            }
        }
        if (removedLines == 0 && index > 1) {
            final int previousLineWithOriginalIndex = text.getLineWithOriginalIndex(index - 1).getContent().length();
            if (previousLineWithOriginalIndex < 10) {

                final String lineWithOriginalIndexMinus2 = text.getLineWithOriginalIndex(index - 2).getContent();
                if (lineContainsADate(lineWithOriginalIndexMinus2)
                        && lineContainsAnEmail(lineWithOriginalIndexMinus2)) {
                    markQuoted(index - 1);
                    markQuoted(index - 2);
                }
            }
        }
    }

    private boolean lineContainsAnEmail(String line) {
        // Is there something that looks like an email?
        return EMAIL_PATTERN.matcher(line).find();
    }

    private boolean lineContainsADate(String line) {
        for (final Pattern datePattern : DATE_PATTERNS) {
            if (datePattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }
}