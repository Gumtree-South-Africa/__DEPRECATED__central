package com.ecg.messagecenter.cleanup;

import java.util.regex.Pattern;

/**
 * Created by pragone on 19/04/15.
 */
public class RemoveDateEmailQuoteHeaderAdvice extends AbstractCleanupAdvice {
    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("[0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4}"),
            Pattern.compile("[0-9]{4}[/-][0-9]{1,2}[/-][0-9]{1,2}"),
            Pattern.compile("[0-9]{1,2} [a-zA-Z]{2,10} [0-9]{4}"),
            Pattern.compile("[a-zA-Z]{2,10} [0-9]{1,2}, [0-9]{4}"),
            Pattern.compile("[0-9]{4}年[0-9]{1,2}月[0-9]{1,2}日")
            //Pattern.compile("(Mon(day)?|Tue(sday)?|Wed(nesday)?|Thu(rsday)?|Fri(day)?|Sat(urday)?|Sun(day)?), [0-9]{1,2} (Jan(uary)?|Feb(ruary)?|Mar(ch)?|Apr(il)?|May|Jun(e)?|Jul(y)?|Aug(ust)?|Sep(tember)?|Oct(ober)?|Nov(ember)?|Dec(ember)?) \\d{4}")
    };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");

    private static final Pattern GUMTREE_AUSTRALIA = Pattern.compile("Gumtree\\s+Australia");

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
            int prevIndex = index -1;
            if (text.getAdvice().isLineCleaned(prevIndex) || text.getAdvice().isLineQuoted(prevIndex)) {
                index--;
                continue; // skip
            }
            String theLine = text.getLineWithOriginalIndex(prevIndex).content;
            if (lineContainsADate(theLine)) {
                markQuoted(prevIndex);
                keepChecking = false;
            } else if (removedLines == 0 && (lineContainsThePattern(theLine, EMAIL_PATTERN) || lineContainsThePattern(theLine, GUMTREE_AUSTRALIA))) {
                markQuoted(prevIndex);
                index--;
                removedLines++;
            } else {
                keepChecking = false;
            }
        }
        if (removedLines == 0 && index > 1) {
            if (text.getLineWithOriginalIndex(index - 1).content.length() < 10
                    && lineContainsADate(text.getLineWithOriginalIndex(index - 2).content)
                    && lineContainsThePattern(text.getLineWithOriginalIndex(index - 2).content, EMAIL_PATTERN)) {
                markQuoted(index - 1);
                markQuoted(index - 2);
            }
        }
    }


    private static boolean lineContainsThePattern(String line, Pattern pattern) {
        // Is there something that looks like an email?
        if (pattern.matcher(line).find()) {
            return  true;
        }
        return false;
    }

    private static boolean lineContainsADate(String line) {
        for (Pattern datePattern : DATE_PATTERNS) {
            if (datePattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }
}
