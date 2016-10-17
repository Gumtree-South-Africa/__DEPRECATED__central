package com.ecg.replyts.core.api.model.user.event;

import com.ecg.replyts.core.api.util.Assert;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class EmailPreferenceEvent extends UserEvent {
    private final EmailPreferenceCommand emailPreferenceCommand;
    private final String userId;

    @JsonCreator
    public EmailPreferenceEvent(@JsonProperty("emailPreferenceCommand") EmailPreferenceCommand emailPreferenceCommand,
                                @JsonProperty("userId") String userId) {
        Assert.notNull(emailPreferenceCommand);
        Assert.notNull(userId);
        this.userId = userId;
        this.emailPreferenceCommand = emailPreferenceCommand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EmailPreferenceEvent that = (EmailPreferenceEvent) o;
        return emailPreferenceCommand == that.emailPreferenceCommand &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), emailPreferenceCommand, userId);
    }

    public EmailPreferenceCommand getEmailPreferenceCommand() {
        return emailPreferenceCommand;
    }

    public String getUserId() {
        return userId;
    }
}
