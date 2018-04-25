package com.ecg.messagecenter.gtuk.cleanup;

public class RemoveLinesThatStartWithQuotesAdvice extends AbstractCleanupAdvice {
    private static final String QUOTE = ">";

    public RemoveLinesThatStartWithQuotesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        quoteByLine(new LineAdvice() {
            @Override
            public boolean isQuoted(Text.Line line) {
                return line.getContent().startsWith(QUOTE);
            }
        });
    }
}