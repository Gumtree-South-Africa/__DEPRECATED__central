package com.ecg.messagecenter.cleanup;

public class RemoveLinesThatStartWithQuotesAdvice extends AbstractCleanupAdvice {
    private static final String QUOTE = ">";
    private static final String PIPE = "|";

    public RemoveLinesThatStartWithQuotesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        quoteByLine(new LineAdvice() {
            @Override
            public boolean isQuoted(Text.Line line) {
                return line.content.startsWith(QUOTE) || line.content.startsWith(PIPE);
            }
        });
    }
}