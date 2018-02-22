package com.ecg.messagecenter.cleanup;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CleanupAppSignatureAdvice extends AbstractCleanupAdvice {

    private static final Pattern SEPARATOR = Pattern.compile("^--$");

    private static final List<Pattern> SIGNATURES = Arrays.asList(
                    Pattern.compile("^Sent from Mail.Ru app for iOS$", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("^Inviato da Libero Mail per Android$",
                                    Pattern.CASE_INSENSITIVE),
                    Pattern.compile("^Le mail ti raggiungono ovunque con BlackBerry® from Vodafone!$",
                                    Pattern.CASE_INSENSITIVE));

    private static final int THRESHOLD = 2;

    protected CleanupAppSignatureAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {

        boolean unleash = false;

        for (Text.Line line : text.lines) {

            if (text.getAdvice().isLineCleaned(line.originalIndex) || text.getAdvice()
                            .isLineQuoted(line.originalIndex)) {
                continue;
            }

            if (unleash) {
                markQuoted(line.originalIndex);
                continue;
            }

            if (hasSeparatorAt(line.originalIndex) && matchThreshold(line.originalIndex)) {
                markQuoted(line.originalIndex);
                unleash = true;
                continue;
            }

            for (Pattern signature : SIGNATURES) {
                if (signature.matcher(line.content).matches()) {
                    markQuoted(line.originalIndex);
                    unleash = true;

                    if (hasSeparatorAt(line.originalIndex - 1)) {
                        markQuoted(line.originalIndex - 1);
                    }
                }
            }
        }
    }

    private boolean hasSeparatorAt(int index) {
        try {
            return SEPARATOR.matcher(text.lines.get(index).content).matches();
        } catch (java.lang.IndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean matchThreshold(int index) {
        return (index + THRESHOLD) >= (text.lines.size() - 1);
    }
}
