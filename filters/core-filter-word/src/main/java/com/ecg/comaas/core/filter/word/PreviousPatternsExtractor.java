package com.ecg.comaas.core.filter.word;

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

    PreviousPatternsExtractor(Conversation conversation,Message currentMessage) {
        this.conversation = conversation;
        this.currentMessage = currentMessage;
    }

    public Set<String> previouselyFiredPatterns() {
        Builder<String> found = ImmutableSet.builder();

        for (Message message : conversation.getMessages()) {
            boolean isCurentMessage = message.getId().equals(currentMessage.getId());
            boolean messageWasNotSent = message.getState() != MessageState.SENT;
            if(isCurentMessage || messageWasNotSent) {
                continue;
            }
            addAllWordfilterHits(message.getProcessingFeedback(), found);
        }
        return found.build();
    }

    private void addAllWordfilterHits(List<ProcessingFeedback> processingFeedback, Builder<String> found) {
        for (ProcessingFeedback feedback : processingFeedback) {
            boolean isWordfilterHit = feedback.getFilterName().equals(WordfilterFactory.IDENTIFIER);
            if(isWordfilterHit) {
                found.add(feedback.getUiHint());
            }
        }
    }
}
