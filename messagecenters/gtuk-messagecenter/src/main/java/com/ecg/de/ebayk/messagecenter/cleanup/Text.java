package com.ecg.de.ebayk.messagecenter.cleanup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pragone on 19/04/15.
 */
public class Text {
    private int originalNumLines = 0;
    private List<Line> lines = new ArrayList<>();
    private AggregatedCleanupAdvice aggregatedAdvice = new AggregatedCleanupAdvice();

    public Text(String textStr) {
        String[] lines = textStr.split("\n");
        for (int i = 0; i < lines.length; i++) {
            this.lines.add(new Line(basicCleanup(lines[i]), i));
        }
        this.originalNumLines = lines.length;
    }

    private String basicCleanup(String line) {
        return line
                .trim()
                .replaceAll("[\\s]+", " ")
                .replaceAll("\\s[^a-zA-Z0-9&]+\\s", " ")
                .replaceAll("\\s[-_*]+\\s", "")
                .replaceAll("^[-_*]+$", "");
    }

    public Text(int originalNumLines, List<Line> lines) {
        this.originalNumLines = originalNumLines;
        this.lines = lines;
    }

    public List<Line> getLines() {
        return lines;
    }

    public AggregatedCleanupAdvice getAggregatedAdvice() {
        return aggregatedAdvice;
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
        return sb.toString();
    }

    public static class Line {
        private String content;
        private int originalIndex;

        public Line(String content, int originalIndex) {
            this.content = content;
            this.originalIndex = originalIndex;
        }

        public String getContent() {
            return content;
        }

        public int getOriginalIndex() {
            return originalIndex;
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
