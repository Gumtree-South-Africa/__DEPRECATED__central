package com.ecg.replyts.app.textcleanup;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Extracts the text that is added by this message, compared to the
 * previous message.
 * In case there is no previous message, the whole message is considered
 * added.
 * TODO: compare against email platform template for new messages.
 */
public final class EmailAddedTextExtractor {

    private final Message message;
    private final PlainTextMailReplyMarkerRemover cleaner = new PlainTextMailReplyMarkerRemover();

    private EmailAddedTextExtractor(Message message) {
        this.message = message;
    }

    public static EmailAddedTextExtractor getNewText(Message message) {
        return new EmailAddedTextExtractor(message);
    }

    public String in(Optional<Message> previousMessage) {
        if (!previousMessage.isPresent())
            return message.getPlainTextBody();

        String currentText = getCurrentText();
        String previousText = getPreviousText(previousMessage);

        List<DiffMatchPatch.Diff> diffs = getDiffs(currentText, previousText);
        List<String> parts = getParts(diffs);

        return Joiner.on(" ").join(parts);
    }

    private List<String> getParts(List<DiffMatchPatch.Diff> diffs) {
        List<String> parts = new ArrayList<String>();
        for (DiffMatchPatch.Diff diff : diffs) {
            if (diff.operation == DiffMatchPatch.Operation.INSERT) {
                parts.add(diff.text.trim());
            }
        }
        return parts;
    }

    private List<DiffMatchPatch.Diff> getDiffs(String cleanedUpText, String cleanedUpTextPrevious) {
        DiffMatchPatch diffMatchPatch = new DiffMatchPatch();
        LinkedList<DiffMatchPatch.Diff> diffs = diffMatchPatch.diff_main(cleanedUpTextPrevious, cleanedUpText); // NOSONAR
        diffMatchPatch.diff_cleanupMerge(diffs);
        return diffs;
    }

    private String getCurrentText() {
        return cleaner.remove(message.getPlainTextBody());
    }

    private String getPreviousText(Optional<Message> previousMessage) {
        return cleaner.remove(previousMessage.get().getPlainTextBody());
    }

}
