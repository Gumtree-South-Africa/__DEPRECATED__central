package com.ecg.messagecenter.cleanup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pragone on 19/04/15.
 */
public class Text {
    private static final int THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY = 10000;
    private static final String TRUNCATION_MSG =
                    "...\r\n\r\nIl tuo messaggio è stato troncato perché troppo lungo. Usa il tuo client email se vuoi leggere il messaggio completo.";

    int originalNumLines = 0;
    List<Line> lines = new ArrayList<>();
    AggregatedCleanupAdvice aggregatedAdvice = new AggregatedCleanupAdvice();

    public Text(String textStr) {
        String[] lines = textStr.split("\n");
        for (int i = 0; i < lines.length; i++) {
            this.lines.add(new Line(basicCleanup(lines[i]), i));
        }
        this.originalNumLines = lines.length;
    }

    private String basicCleanup(String line) {
        return line.trim();
    }

    public Text(int originalNumLines, List<Line> lines) {
        this.originalNumLines = originalNumLines;
        this.lines = lines;
    }

    public int getOriginalNumLines() {
        return originalNumLines;
    }

    public void addAdvice(CleanupAdvice cleanupAdvice) {
        this.aggregatedAdvice.addAdvice(cleanupAdvice);
    }

    public Text getEffectiveText() {
        List<Line> remainingLines = new ArrayList<>();
        for (int i = 0; i < this.originalNumLines; i++) {
            if (!this.aggregatedAdvice.isLineQuoted(i) && !this.aggregatedAdvice.isLineCleaned(i)) {
                remainingLines.add(this.getLineWithOriginalIndex(i));
            }
        }
        return new Text(originalNumLines, remainingLines);
    }

    public Line getLineWithOriginalIndex(int originalIndex) {
        for (Line line : lines) {
            if (line.originalIndex == originalIndex) {
                return line;
            }
        }
        return null;
    }



    public String getAsString() {
        StringBuilder sb = new StringBuilder();
        for (Line line : getEffectiveText().lines) {
            sb.append(line.content);
            sb.append('\n');
        }

        String result = sb.toString().trim();
        if (result.length() > THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY) {
            return result.substring(0, THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY) + TRUNCATION_MSG;
        }

        return result;
    }

    public static class Line {
        public String content;
        public int originalIndex;

        public Line(String content, int originalIndex) {
            this.content = content;
            this.originalIndex = originalIndex;
        }
    }

    public int getFirstQuoteIndex() {
        for (int i = 0; i < originalNumLines; i++) {
            if (this.aggregatedAdvice.isLineQuoted(i)) {
                return i;
            }
        }
        return -1;
    }

    public CleanupAdvice getAdvice() {
        return this.aggregatedAdvice;
    }
}
