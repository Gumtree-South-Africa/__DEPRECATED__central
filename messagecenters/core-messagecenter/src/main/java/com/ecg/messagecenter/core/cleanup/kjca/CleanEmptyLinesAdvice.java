package com.ecg.messagecenter.core.cleanup.kjca;

/**
 * Created by pragone on 19/04/15.
 */
public class CleanEmptyLinesAdvice extends AbstractCleanupAdvice {
    public CleanEmptyLinesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        cleanByLine(line -> line.content.isEmpty());
    }
}
