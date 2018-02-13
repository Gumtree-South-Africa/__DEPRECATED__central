package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreviousPatternsExtractor {
    private Conversation conversation;

    private Message currentMessage;

    public PreviousPatternsExtractor(Conversation conversation, Message currentMessage) {
        this.conversation = conversation;
        this.currentMessage = currentMessage;
    }

    public Set<String> previouselyFiredPatterns() {
        Set<String> found = new HashSet<>();

        for (Message message : conversation.getMessages()) {
            boolean isCurrentMessage = message.getId().equals(currentMessage.getId());
            boolean messageWasNotSent = message.getState() != MessageState.SENT;

            if (isCurrentMessage || messageWasNotSent) {
                continue;
            }

            found.addAll(findAllWordfilterHits(message.getProcessingFeedback()));
        }

        return found;
    }

    private Set<String> findAllWordfilterHits(List<ProcessingFeedback> processingFeedback) {
        Set<String> result = new HashSet<>();

        for (ProcessingFeedback feedback : processingFeedback) {
            if (feedback.getFilterName().equals(WordfilterFactory.class.getName())) {
                result.add(feedback.getUiHint());
            }
        }

        return result;
    }
}
