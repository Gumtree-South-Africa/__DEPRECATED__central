package com.ecg.messagecenter.cleanup;

import java.util.regex.Pattern;

/**
 * Created by jaludden on 10/12/15.
 */
public class CleanupKijijiFirstReplyZapiTemplateAdvice extends AbstractCleanupAdvice {

    private static final Pattern TOP_TEMPLATE = Pattern.compile("----------");

    private static final Pattern BOTTOM_TEMPLATE = Pattern.compile("----------");

    private static final int START = 0;
    private static final int END = 1;

    protected CleanupKijijiFirstReplyZapiTemplateAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        boolean toRemove = false;
        boolean topFound = false;
        for (Text.Line line : text.lines) {

            if (TOP_TEMPLATE.matcher(line.content).matches() && !topFound) {
                markQuoted(line.originalIndex);
                for (int i = 0; i < line.originalIndex; i++) {
                    markQuoted(i);
                }
                topFound = true;
                toRemove = false;
            } else if (topFound && BOTTOM_TEMPLATE.matcher(line.content).matches()) {
                markQuoted(line.originalIndex);
                toRemove = true;
            }

            if (toRemove) {
                markQuoted(line.originalIndex);
            }
        }
    }
}
