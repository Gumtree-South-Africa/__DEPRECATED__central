package com.ecg.messagecenter.it.cleanup;

import java.util.regex.Pattern;

/**
 * Created by jaludden on 21/01/16.
 */
public class CleanupKijijiOldTemplateAdvice extends AbstractCleanupAdvice {

    private static final Pattern TOP_TEMPLATE =
                    Pattern.compile("è interessato al tuo annuncio \\\"\\\":");
    private static final Pattern BOTTOM_TEMPLATE =
                    Pattern.compile("Per tutelare ancor più la tua privacy, mascheriamo l'indirizzo email di chi si scambia messaggi in");

    protected CleanupKijijiOldTemplateAdvice(Text text) {
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
