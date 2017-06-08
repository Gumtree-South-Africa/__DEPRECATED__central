package com.ecg.messagecenter.cleanup;

import java.util.regex.Pattern;

/**
 * Created by jaludden on 10/12/15.
 */
public class CleanupKijijiFirstReplyTemplateAdvice extends AbstractCleanupAdvice {

    private static final Pattern TOP_TEMPLATE = Pattern.compile("Ecco il suo messaggio:");

    private static final Pattern BOTTOM_TEMPLATE =
                    Pattern.compile("Per rispondere a questo messaggio usa il tasto Rispondi del tuo programma di posta");

    private static final int START = 0;
    private static final int END = 1;

    protected CleanupKijijiFirstReplyTemplateAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        boolean toRemove = false;
        for (Text.Line line : text.lines) {

            if (TOP_TEMPLATE.matcher(line.content).matches() && !text.getAdvice()
                            .isLineQuoted(line.originalIndex)) {
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
