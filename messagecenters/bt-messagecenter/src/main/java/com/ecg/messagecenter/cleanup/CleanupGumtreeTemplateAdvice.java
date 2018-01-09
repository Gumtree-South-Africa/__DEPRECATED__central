package com.ecg.messagecenter.cleanup;

import java.util.regex.Pattern;

/**
 * Created by mdarapour.
 */
public class CleanupGumtreeTemplateAdvice extends AbstractCleanupAdvice {
    private static final Pattern[] TOP_TEMPLATE = {
            Pattern.compile("[\\w\\s]+replied to your ad:"),
        Pattern.compile("Respond to[\\w\\s]+by replying directly to this email")
    };
    private static final Pattern[] BOTTOM_TEMPLATE = {
            Pattern.compile("Already sold it\\?"),
            Pattern.compile("(Gumtree member since [0-9]{4}|Offered to pay with|Agreed on a price\\? When you meet in person, ask your buyer to pay with the PayPal app\\.|Already sold it\\?)")
    };

    private static final int START = 0;
    private static final int END = 1;


    protected CleanupGumtreeTemplateAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        boolean finding = false;
        for (Text.Line line : text.lines) {
            if (text.getAdvice().isLineCleaned(line.originalIndex) ||
                    text.getAdvice().isLineQuoted(line.originalIndex)) {
                continue; // Ignore already cleaned lights
            }
            finding = markQuotedFromStartQuotedLine(TOP_TEMPLATE, line , finding);
            finding = markQuotedFromStartQuotedLine(BOTTOM_TEMPLATE, line, finding);
        }
    }

    private boolean markQuotedFromStartQuotedLine(Pattern[] patterns, Text.Line line, boolean finding) {
        if(patterns[START].matcher(line.content).matches()) {
            // Try to remove the sender name by traversing back to the last uncleaned line
            if(line.originalIndex > 0) {
                int i = 1;
                while(text.getAdvice().isLineCleaned(line.originalIndex-i)) i++;
                markQuoted(line.originalIndex-i);
            }
            markQuoted(line.originalIndex);
            return true;
        }
        if (patterns[END].matcher(line.content).matches()) {
            markQuoted(line.originalIndex);
            return false;
        }

        if(finding) {
            markQuoted(line.originalIndex);
        }

        return finding;
    }
}
