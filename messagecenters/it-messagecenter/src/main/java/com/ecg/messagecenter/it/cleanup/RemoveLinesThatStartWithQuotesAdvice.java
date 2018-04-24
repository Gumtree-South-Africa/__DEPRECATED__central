package com.ecg.messagecenter.it.cleanup;

/**
 * Created by pragone on 19/04/15.
 */
public class RemoveLinesThatStartWithQuotesAdvice extends AbstractCleanupAdvice {

    private static final String QUOTE = ">";

    public RemoveLinesThatStartWithQuotesAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {
        quoteByLine(new LineAdvice() {
            @Override public boolean isQuoted(Text.Line line) {
                return line.content.startsWith(QUOTE);
            }
        });
    }
}
