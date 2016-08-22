package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class ConversationClosedEvent extends ConversationEvent {

    private ConversationRole closeIssuer;

    protected ConversationClosedEvent(@JsonProperty("conversationModifiedAt") DateTime decidedAt, @JsonProperty("closeIssuer")  ConversationRole closeIssuer) {
        super(ConversationClosedEvent.class.getSimpleName() + "-" + decidedAt.getMillis(), decidedAt);
        this.closeIssuer = closeIssuer;
    }

    public ConversationClosedEvent(ConversationClosedCommand cmd) {
        this(cmd.getAt(), cmd.getCloseIssuer());
    }

    public ConversationRole getCloseIssuer() {
        return closeIssuer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationClosedEvent that = (ConversationClosedEvent) o;

        return Pairwise.pairsAreEqual(this.getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis(),
                this.getCloseIssuer(), that.getCloseIssuer());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getConversationModifiedAt().getMillis(),getCloseIssuer());
    }
}
