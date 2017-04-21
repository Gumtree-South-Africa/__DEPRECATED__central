package com.ecg.de.ebayk.messagecenter.cleanup;

import java.util.regex.Pattern;

/**
 * Created by mdarapour.
 */
public class CleanSignatureLinesAdvice extends AbstractCleanupAdvice {
    private static final Pattern[] SIGNATURE_PATTERNS = {Pattern.compile(
                    "^(sent from|inviato da) (iphone|ipad|windows mail|windows phone|samsung (mobile|tablet)|HTC)[\\.]?",
                    Pattern.CASE_INSENSITIVE),
                    Pattern.compile("^(sent from my|inviato dal mio) (iphone|ipad|windows mail|windows phone|samsung (mobile|tablet)|HTC)[\\.]?",
                                    Pattern.CASE_INSENSITIVE)};

    public CleanSignatureLinesAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        boolean finding = false;
        for (Text.Line line : text.lines) {
            if (text.getAdvice().isLineCleaned(line.originalIndex) || text.getAdvice()
                            .isLineQuoted(line.originalIndex)) {
                continue; // Ignore already cleaned lights
            }

            for (Pattern pattern : SIGNATURE_PATTERNS) {
                if (pattern.matcher(line.content).matches()) {
                    finding = true;
                }
            }

            if (finding) {
                markQuoted(line.originalIndex);
            }
        }
    }
}
