package com.ecg.messagebox.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.joda.time.base.BaseDateTime;

import java.util.Objects;
import java.util.Optional;

public class ConversationMetadata {

    private String emailSubject;
    private Optional<DateTime> creationDate = Optional.empty();

    @JsonCreator
    public ConversationMetadata(@JsonProperty("creationDate") DateTime creationDate,
                                @JsonProperty("emailSubject") String emailSubject) {
        this.creationDate = Optional.ofNullable(creationDate);
        this.emailSubject = emailSubject;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public Optional<DateTime> getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationMetadata that = (ConversationMetadata) o;
        return Objects.equals(emailSubject, that.emailSubject)
                && Objects.equals(creationDate.map(BaseDateTime::getMillis), that.creationDate.map(BaseDateTime::getMillis));
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailSubject, creationDate);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("emailSubject", emailSubject)
                .add("creationDate", creationDate)
                .toString();
    }
}