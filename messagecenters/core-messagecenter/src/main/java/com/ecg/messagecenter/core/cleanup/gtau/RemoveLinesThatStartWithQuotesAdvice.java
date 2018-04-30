package com.ecg.messagecenter.core.cleanup.gtau;

public class RemoveLinesThatStartWithQuotesAdvice extends AbstractCleanupAdvice {
    private static final String QUOTE = ">";
    private static final String PIPE = "|";

    public RemoveLinesThatStartWithQuotesAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {
        quoteByLine(line -> line.content.startsWith(QUOTE) || line.content.startsWith(PIPE));
    }
}