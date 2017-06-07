package com.ecg.messagecenter.cleanup;

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
        if(index < cleaned.length)
            return quoted[index];
        return false;
    }

    @Override
    public boolean isLineCleaned(int index) {
        if(index < cleaned.length)
            return cleaned[index];
        return false;
    }

    public void quoteByLine(LineAdvice lineAdvice) {
        for (Text.Line line : text.lines) {
            if (lineAdvice.isQuoted(line)) {
                this.markQuoted(line.originalIndex);
            }
        }
    }
    public void cleanByLine(LineAdvice lineAdvice) {
        for (Text.Line line : text.lines) {
            if (lineAdvice.isQuoted(line)) {
                this.markCleaned(line.originalIndex);
            }
        }
    }

    public interface LineAdvice {
        public boolean isQuoted(Text.Line line);
    }
}