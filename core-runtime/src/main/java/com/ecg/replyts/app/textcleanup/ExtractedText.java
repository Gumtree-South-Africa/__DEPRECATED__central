package com.ecg.replyts.app.textcleanup;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Optional;

/**
 * This class unpackIfGzipped a diff from a given message, base on give conversation to find previous message and diff them.
 * The result can by used by processing in filter. This can be used to avoid double process same content.
 */
public final class ExtractedText {

    private final PreviousMessageFinder previousMessageFinder;
    private final Message currentMessage;

    ExtractedText(Message currentMessage) {
        this(currentMessage, new PreviousMessageFinder());
    }

    ExtractedText(Message currentMessage, PreviousMessageFinder messageFinder) {
        this.previousMessageFinder = messageFinder;
        this.currentMessage = currentMessage;
    }


    public static ExtractedText getNewText(Message message) {
        return new ExtractedText(message);
    }

    public String in(Conversation conversation) {
        Optional<Message> previousMessage = previousMessageFinder.previousMessage(currentMessage, conversation);
        if (!previousMessage.isPresent()) {
            return currentMessage.getPlainTextBody();
        }

        return EmailAddedTextExtractor.getNewText(currentMessage).in(previousMessage);
    }

}
