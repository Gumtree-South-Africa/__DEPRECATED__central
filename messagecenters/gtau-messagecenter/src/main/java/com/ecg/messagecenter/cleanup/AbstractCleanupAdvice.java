package com.ecg.messagecenter.cleanup;

/**
 * Created by pragone on 19/04/15.
 */
public abstract class AbstractCleanupAdvice implements CleanupAdvice {

    protected final Text text;
    public boolean[] quoted;
    public boolean[] cleaned;

    protected AbstractCleanupAdvice(Text text) {
        this.text = text;
        this.quoted = new boolean[text.getOriginalNumLines()];
        this.cleaned = new boolean[text.getOriginalNumLines()];
        this.processAdvice();
    }

    public abstract void processAdvice();

    protected void markQuoted(int index) {
        this.quoted[index] = true;
    }

    protected void markCleaned(int index) {
        this.cleaned[index] = true;
    }

    @Override
    public boolean isLineQuoted(int index) {
        return quoted[index];
    }

    @Override
    public boolean isLineCleaned(int index) {
        return cleaned[index];
    }

    public void quoteByLine(LineAdvice lineAdvice) {
        for (com.ecg.messagecenter.cleanup.Text.Line line : text.lines) {
            if (lineAdvice.isQuoted(line)) {
                this.markQuoted(line.originalIndex);
            }
        }
    }
    public void cleanByLine(LineAdvice lineAdvice) {
        for (com.ecg.messagecenter.cleanup.Text.Line line : text.lines) {
            if (lineAdvice.isQuoted(line)) {
                this.markCleaned(line.originalIndex);
            }
        }
    }

    public interface LineAdvice {
        public boolean isQuoted(com.ecg.messagecenter.cleanup.Text.Line line);
    }
}
