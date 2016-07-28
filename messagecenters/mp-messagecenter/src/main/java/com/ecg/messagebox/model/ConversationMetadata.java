package com.ecg.messagebox.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

public class ConversationMetadata {

//    private DateTime creationDate;
//    private String initiatorUserId;

    private String emailSubject;

    @JsonCreator
    public ConversationMetadata(@JsonProperty("emailSubject") String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationMetadata metadata = (ConversationMetadata) o;
        return Objects.equals(emailSubject, metadata.emailSubject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailSubject);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("emailSubject", emailSubject)
                .toString();
    }
}