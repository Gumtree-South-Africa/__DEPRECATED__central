package com.ecg.messagecenter.cleanup;

public class CleanEmptyLinesAdvice extends AbstractCleanupAdvice {
    public CleanEmptyLinesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        cleanByLine(new LineAdvice() {
            @Override
            public boolean isQuoted(Text.Line line) {
                return line.content.isEmpty();
            }
        });
    }
}