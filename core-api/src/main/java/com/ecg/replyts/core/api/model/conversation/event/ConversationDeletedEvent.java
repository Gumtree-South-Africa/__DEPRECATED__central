package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class ConversationDeletedEvent extends ConversationEvent {

    public ConversationDeletedEvent(@JsonProperty("conversationModifiedAt") DateTime decidedAt) {
        super(ConversationDeletedEvent.class.getSimpleName() + "-" + decidedAt.getMillis(), decidedAt);
    }

    public ConversationDeletedEvent(ConversationDeletedCommand cmd) {
        this(cmd.getAt());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationDeletedEvent that = (ConversationDeletedEvent) o;

        return Pairwise.pairsAreEqual(this.getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getConversationModifiedAt().getMillis());
    }
}
