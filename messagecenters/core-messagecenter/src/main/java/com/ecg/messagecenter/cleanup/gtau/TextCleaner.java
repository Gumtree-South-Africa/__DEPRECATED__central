package com.ecg.messagecenter.cleanup.gtau;

public final class TextCleaner {

    private static final String MARKUP_HINT = "!important;";

    private TextCleaner() {
    }

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

        return cleanupUncleanedMarkup(text);
    }

    private static String cleanupUncleanedMarkup(Text text) {
        String str = text.getAsString();
        if (str.contains(MARKUP_HINT)) {
            String substrToReplace = str.substring(0, str.lastIndexOf(MARKUP_HINT) + MARKUP_HINT.length());
            return str.replace(substrToReplace, "");
        }
        return str;
    }
}
