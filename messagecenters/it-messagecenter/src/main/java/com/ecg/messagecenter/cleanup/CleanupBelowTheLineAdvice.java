package com.ecg.messagecenter.cleanup;

import java.util.regex.Pattern;

public class CleanupBelowTheLineAdvice extends AbstractCleanupAdvice {

    private static final Pattern[] bottomOfTheBarrel = { // WTF!?!
                    Pattern.compile("^Il .*,[\\s\".*\"]? [<]?.*@.*[>]?(\\sha(\\sscritto:)?)?"),
                    Pattern.compile("^Il .*[0-9]{4} [0-9]{1,2}:[0-9]{1,2}(\\s(AM|PM))?, \".*\"? <.*"),
                    Pattern.compile("^On .* [<]?.*@.*[>]? wrote:")};

    protected CleanupBelowTheLineAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        boolean scrape = false;

        for (Pattern pattern : bottomOfTheBarrel) {
            for (Text.Line line : text.lines) {
                if (text.getAdvice().isLineQuoted(line.originalIndex)) {
                    continue;
                }

                if (scrape) {
                    markQuoted(line.originalIndex);
                    continue;
                }

                if (pattern.matcher(line.content).matches()) {
                    markQuoted(line.originalIndex);
                    scrape = true;
                }
            }

            if (scrape) {
                return;
            }
        }
    }
}
