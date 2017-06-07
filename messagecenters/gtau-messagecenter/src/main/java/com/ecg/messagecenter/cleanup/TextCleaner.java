package com.ecg.messagecenter.cleanup;

public class TextCleaner {
    public static String cleanupText(String originalText) {
        Text text = new Text(originalText);
        text.addAdvice(new RemoveLinesThatStartWithQuotesAdvice(text));
        text.addAdvice(new CleanEmptyLinesAdvice(text));
        text.addAdvice(new RemoveDateEmailQuoteHeaderAdvice(text));
        text.addAdvice(new CleanupWithQuotedHeadersAdvice(text));
        text.addAdvice(new CleanupWithSeparatorAdvice(text));
        text.addAdvice(new CleanSignatureLinesAdvice(text));
        text.addAdvice(new CleanupGumtreeTemplateAdvice(text));
        text.addAdvice(new CleanupGumtreeReplyToAdAdvice(text));
        text.addAdvice(new CleanupGumtreeDealerLeadAdvice(text));
        return text.getAsString();
    }
}