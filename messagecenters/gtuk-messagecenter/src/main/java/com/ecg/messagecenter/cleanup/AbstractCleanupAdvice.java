package com.ecg.messagecenter.cleanup;

public abstract class AbstractCleanupAdvice implements CleanupAdvice {
    protected final Text text;
    private boolean[] quoted;
    private boolean[] cleaned;

    protected AbstractCleanupAdvice(Text text) {
        this.text = text;
        this.quoted = new boolean[text.getOriginalNumLines()];
        this.cleaned = new boolean[text.getOriginalNumLines()];
        this.processAdvice();
    }

    public void setQuoted(int index, boolean value) {
        this.quoted[index] = value;
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

    public void quoteByLine(final LineAdvice lineAdvice) {
        for (final Text.Line line : text.getLines()) {
            if (lineAdvice.isQuoted(line)) {
                this.markQuoted(line.getOriginalIndex());
            }
        }
    }

    public void cleanByLine(LineAdvice lineAdvice) {
        for (final Text.Line line : text.getLines()) {
            if (lineAdvice.isQuoted(line)) {
                this.markCleaned(line.getOriginalIndex());
            }
        }
    }

    public interface LineAdvice {
        public boolean isQuoted(Text.Line line);
    }
}