package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedAndDeletedForUserCommand;
import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class ConversationClosedAndDeletedForUserEvent extends ConversationEvent {

    private String userId;
    private String userEmail;

    protected ConversationClosedAndDeletedForUserEvent(@JsonProperty("conversationModifiedAt") DateTime decidedAt, @JsonProperty("issuerId")  String userId, @JsonProperty("issuerEmail") String userEmail) {
        super(ConversationClosedAndDeletedForUserEvent.class.getSimpleName() + "-" + decidedAt.getMillis(), decidedAt);
        this.userId = userId;
        this.userEmail = userEmail;
    }

    public ConversationClosedAndDeletedForUserEvent(ConversationClosedAndDeletedForUserCommand cmd) {
        this(cmd.getAt(), cmd.getUserId(), cmd.getUserEmail());
    }

    public String getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConversationClosedAndDeletedForUserEvent that = (ConversationClosedAndDeletedForUserEvent) o;

        return Pairwise.pairsAreEqual(this.getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis(),
                this.getUserId(), that.getUserId(),
                this.getUserEmail(), that.getUserEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getConversationModifiedAt().getMillis(), getUserId(), getUserEmail());
    }
}
