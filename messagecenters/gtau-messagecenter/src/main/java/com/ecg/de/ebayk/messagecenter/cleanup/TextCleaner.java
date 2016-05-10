package com.ecg.de.ebayk.messagecenter.cleanup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pragone on 19/04/15.
 */
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
