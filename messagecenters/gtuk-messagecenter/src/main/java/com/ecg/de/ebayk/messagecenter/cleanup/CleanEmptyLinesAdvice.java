package com.ecg.de.ebayk.messagecenter.cleanup;

/**
 * Created by pragone on 19/04/15.
 */
public class CleanEmptyLinesAdvice extends AbstractCleanupAdvice {
    public CleanEmptyLinesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        cleanByLine(new LineAdvice() {
            @Override
            public boolean isQuoted(Text.Line line) {
                return line.getContent().isEmpty();
            }
        });
    }
}
