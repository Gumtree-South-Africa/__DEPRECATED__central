package com.ecg.messagecenter.cleanup;

/**
 * Specific advice to extract the 'Reply To Ad' message.
 */
public class CleanupGumtreeReplyToAdAdvice extends AbstractCleanupAdvice {

    private static final String REPLIED_TO_YOUR_AD = "replied to your ad";

    private static final String RESPOND_BY_REPLYING_DIRECTLY_TO_THIS_EMAIL =
                    "Respond by replying directly to this email";

    private static final String GUMMIE_SINCE = "Gummie since";

    protected CleanupGumtreeReplyToAdAdvice(Text text) {
        super(text);
    }

    @Override public void processAdvice() {

        boolean isReplyToAdLineDetected = false;
        boolean isReplyToAdMessageLine = false;
        for (Text.Line line : text.lines) {

            if (!text.getAdvice().isLineCleaned(line.originalIndex) && !text.getAdvice()
                            .isLineQuoted(line.originalIndex)) {

                if (line.content.contains(REPLIED_TO_YOUR_AD)) {
                    isReplyToAdMessageLine = true;
                    isReplyToAdLineDetected = true;
                    markQuoted(line.originalIndex);
                    // Clean up by traversing back to the last uncleaned line
                    if (line.originalIndex > 0) {
                        int i = 1;
                        while (text.getAdvice().isLineCleaned(line.originalIndex - i)) {
                            markQuoted(line.originalIndex - ++i);
                        }
                    }
                    // assumption: that buyer name will be the first non-empty line after REPLIED_TO_YOUR_AD
                    // empty lines will be already cleaned, so we iterate over them and find location of buyer name
                    // and THEN we quote the name
                    int i = 1;
                    while (text.getAdvice().isLineCleaned(line.originalIndex + i)) {
                        i++;
                    }
                    markQuoted(line.originalIndex + i);
                } else if (line.content.contains(RESPOND_BY_REPLYING_DIRECTLY_TO_THIS_EMAIL)) {
                    isReplyToAdMessageLine = false;
                    markQuoted(line.originalIndex);
                } else if (isReplyToAdMessageLine) {
                    if (line.content.contains(GUMMIE_SINCE)) {
                        markQuoted(line.originalIndex);
                    }
                } else if (isReplyToAdLineDetected && !isReplyToAdMessageLine) {
                    markQuoted(line.originalIndex);
                }
            }
        }

    }
}
