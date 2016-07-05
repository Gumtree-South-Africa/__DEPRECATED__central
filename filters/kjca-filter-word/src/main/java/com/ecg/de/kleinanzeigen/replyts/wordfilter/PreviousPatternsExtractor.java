package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import java.util.List;
import java.util.Set;

class PreviousPatternsExtractor {
    private final Conversation conversation;
    private final Message currentMessage;

    PreviousPatternsExtractor(Conversation conversation, Message currentMessage) {
        this.conversation = conversation;
        this.currentMessage = currentMessage;
    }

    Set<String> previouslyFiredPatterns() {
        Builder<String> found = ImmutableSet.builder();

        for (Message message : conversation.getMessages()) {
            boolean isCurrentMessage = message.getId().equals(currentMessage.getId());
            boolean messageWasNotSent = message.getState() != MessageState.SENT;
            if (isCurrentMessage || messageWasNotSent) {
                continue;
            }
            addAllWordfilterHits(message.getProcessingFeedback(), found);
        }
        return found.build();
    }

    private void addAllWordfilterHits(List<ProcessingFeedback> processingFeedback, Builder<String> found) {
        for (ProcessingFeedback feedback : processingFeedback) {
            boolean isWordfilterHit = feedback.getFilterName().equals(WordfilterFactory.class.getName());
            if (isWordfilterHit) {
                found.add(feedback.getUiHint());
            }
        }
    }
}
