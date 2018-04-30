package com.ecg.messagecenter.core.cleanup.gtau;

public class CleanEmptyLinesAdvice extends AbstractCleanupAdvice {
    public CleanEmptyLinesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        cleanByLine(line -> line.content.isEmpty());
    }
}