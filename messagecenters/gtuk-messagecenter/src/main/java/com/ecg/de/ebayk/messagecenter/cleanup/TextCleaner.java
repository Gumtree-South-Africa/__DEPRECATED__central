package com.ecg.de.ebayk.messagecenter.cleanup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pragone on 19/04/15.
 */
public final class TextCleaner {
    private TextCleaner() {
    }

    private static final Pattern INITIAL_START = Pattern.compile("Respond to .* by replying directly to this email");
    private static final Pattern INITIAL_END = Pattern.compile("Already sold it\\?");
    private static final Pattern GUMTREE_MEMBER = Pattern.compile("Gumtree member since [0-9]{4}");

    public static String cleanupText(String originalText) {
        Text text = new Text(originalText);
        text.addAdvice(new RemoveLinesThatStartWithQuotesAdvice(text));
        text.addAdvice(new CleanEmptyLinesAdvice(text));
        text.addAdvice(new RemoveDateEmailQuoteHeaderAdvice(text));
        text.addAdvice(new CleanupWithQuotedHeadersAdvice(text));
        text.addAdvice(new CleanupWithSeparatorAdvice(text));
        return text.getAsString();
    }

    public static String cleanupGumtreeFirst(String finalCleanup) {
        Matcher matcher = INITIAL_START.matcher(finalCleanup);
        if (matcher.find()) {
            finalCleanup = finalCleanup.substring(matcher.end());
        }
        matcher = INITIAL_END.matcher(finalCleanup);
        if (matcher.find()) {
            finalCleanup = finalCleanup.substring(0, matcher.start());
        }
        matcher = GUMTREE_MEMBER.matcher(finalCleanup);
        if (matcher.find()) {
            finalCleanup = finalCleanup.substring(0, matcher.start());
        }
        return finalCleanup;
    }

}
