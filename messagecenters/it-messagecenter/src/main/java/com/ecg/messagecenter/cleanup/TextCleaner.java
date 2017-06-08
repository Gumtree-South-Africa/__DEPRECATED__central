package com.ecg.messagecenter.cleanup;

/**
 * Created by pragone on 19/04/15.
 */
public abstract class TextCleaner {

    private static TextCleaner instance;

    protected TextCleaner() {
    }

    public String internalCleanupText(String originalText) {
        Text text = new Text(originalText);
        text.addAdvice(new RemoveLinesThatStartWithQuotesAdvice(text));
        text.addAdvice(new CleanEmptyLinesAdvice(text));
        text.addAdvice(new RemoveDateEmailQuoteHeaderAdvice(text));
        text.addAdvice(new CleanupWithQuotedHeadersAdvice(text));
        text.addAdvice(new CleanupWithSeparatorAdvice(text));
        text.addAdvice(new CleanSignatureLinesAdvice(text));

        specificCleanup(text);
        return text.getAsString();
    }

    protected abstract void specificCleanup(Text text);

    public static class GumtreeTextCleaner extends TextCleaner {

        @Override protected void specificCleanup(Text text) {
            text.addAdvice(new CleanupGumtreeTemplateAdvice(text));
            text.addAdvice(new CleanupGumtreeReplyToAdAdvice(text));
            text.addAdvice(new CleanupGumtreeDealerLeadAdvice(text));
        }
    }


    public static class KijijiTextCleaner extends TextCleaner {

        @Override protected void specificCleanup(Text text) {
            text.addAdvice(new CleanupBelowTheLineAdvice(text)); // keep me up ;)
            text.addAdvice(new CleanupKijijiTemplateAdvice(text));
            text.addAdvice(new CleanupKijijiOldTemplateAdvice(text));
            text.addAdvice(new CleanupKijijiReplyTemplateAdvice(text));
            text.addAdvice(new CleanupKijijiFirstReplyTemplateAdvice(text));
            text.addAdvice(new CleanupKijijiFirstReplyZapiTemplateAdvice(text));
            text.addAdvice(new CleanupAppSignatureAdvice(text)); // keep me down :)
            text.addAdvice(new AllowedMarkupAdvice(text)); // keep me the very last :)
        }
    }

    public static String cleanupText(String originalText) {
        return getInstance().internalCleanupText(originalText);
    }

    private static TextCleaner getInstance() {
        if (instance == null) {
            instance = new KijijiTextCleaner();
        }
        return instance;
    }

    public static void setInstance(TextCleaner instance) {
        TextCleaner.instance = instance;
    }
}
