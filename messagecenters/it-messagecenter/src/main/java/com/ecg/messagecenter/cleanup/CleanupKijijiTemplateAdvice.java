package com.ecg.messagecenter.cleanup;

import java.util.regex.Pattern;

/**
 * Created by jaludden on 10/12/15.
 */
public class CleanupKijijiTemplateAdvice extends AbstractCleanupAdvice {

    private static final Pattern TOP_TEMPLATE = Pattern.compile("<!-- RTS-MESSSAGE-START -->");

    private static final Pattern BOTTOM_TEMPLATE = Pattern.compile("<!-- RTS-MESSSAGE-END -->");

    private static final int START = 0;
    private static final int END = 1;

    protected CleanupKijijiTemplateAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        boolean toRemove = false;
        for (Text.Line line : text.lines) {

            if (TOP_TEMPLATE.matcher(line.content).matches()) {
                markQuoted(line.originalIndex);
                for (int i = 0; i < line.originalIndex; i++) {
                    markQuoted(i);
                }
                toRemove = false;
            }

            if (BOTTOM_TEMPLATE.matcher(line.content).matches()) {
                markQuoted(line.originalIndex);
                toRemove = true;
            }

            if (toRemove) {
                markQuoted(line.originalIndex);
            }
        }
    }
}
