package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedAndDeletedForUserCommand;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class ConversationClosedAndDeletedForUserEvent extends ConversationEvent {

    private String userId;

    protected ConversationClosedAndDeletedForUserEvent(@JsonProperty("conversationModifiedAt") DateTime decidedAt, @JsonProperty("closeIssuer")  String userId) {
        super(ConversationClosedAndDeletedForUserEvent.class.getSimpleName() + "-" + decidedAt.getMillis(), decidedAt);
        this.userId = userId;
    }

    public ConversationClosedAndDeletedForUserEvent(ConversationClosedAndDeletedForUserCommand cmd) {
        this(cmd.getAt(), cmd.getUserId());
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationClosedAndDeletedForUserEvent that = (ConversationClosedAndDeletedForUserEvent) o;

        return Pairwise.pairsAreEqual(this.getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis(),
                this.getUserId(), that.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getConversationModifiedAt().getMillis(), getUserId());
    }
}
