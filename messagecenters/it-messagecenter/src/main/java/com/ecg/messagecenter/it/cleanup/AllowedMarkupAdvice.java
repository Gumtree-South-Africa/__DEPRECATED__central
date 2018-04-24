package com.ecg.messagecenter.it.cleanup;

import java.util.*;
import java.util.regex.Pattern;

public class AllowedMarkupAdvice extends AbstractCleanupAdvice {

    private static final List<Marker> MARKERS =
                    Arrays.asList(new Marker(Pattern.compile("[\n]+", Pattern.CASE_INSENSITIVE),
                                    "\n"), new Marker(Pattern
                                    .compile("<br[\\s+]?[/]?>", Pattern.CASE_INSENSITIVE), "\n"),
                                    new Marker(Pattern.compile("&lt;br[\\s+]?[/]?&gt;",
                                                    Pattern.CASE_INSENSITIVE), "\n"));

    protected AllowedMarkupAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        for (Text.Line line : text.lines) {

            if (text.getAdvice().isLineCleaned(line.originalIndex) || text.getAdvice()
                            .isLineQuoted(line.originalIndex)) {
                continue;
            }

            for (Marker marker : MARKERS) {
                line.content = marker.apply(line.content);
            }
        }
    }

    static class Marker {

        private Pattern pattern = null;
        private String replacement = null;

        public Marker(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        public String apply(String string) {
            return this.pattern.matcher(string).replaceAll(this.replacement);
        }
    }
}
