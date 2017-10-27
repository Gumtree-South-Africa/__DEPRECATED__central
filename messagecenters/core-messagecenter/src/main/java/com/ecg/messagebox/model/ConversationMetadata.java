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
    private Optional<String> title = Optional.empty();
    private String imageUrl;

    @JsonCreator
    public ConversationMetadata(@JsonProperty("creationDate") DateTime creationDate,
                                @JsonProperty("emailSubject") String emailSubject,
                                @JsonProperty("title") String title,
                                @JsonProperty("imageUrl") String imageUrl) {
        this.creationDate = Optional.ofNullable(creationDate);
        this.emailSubject = emailSubject;
        this.title = Optional.ofNullable(title);
        this.imageUrl = imageUrl;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public Optional<DateTime> getCreationDate() {
        return creationDate;
    }

    public Optional<String> getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationMetadata that = (ConversationMetadata) o;
        return Objects.equals(emailSubject, that.emailSubject)
                && Objects.equals(creationDate.map(BaseDateTime::getMillis), that.creationDate.map(BaseDateTime::getMillis))
                && Objects.equals(title, that.title) && Objects.equals(imageUrl, that.imageUrl) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailSubject, creationDate, title, imageUrl);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("emailSubject", emailSubject)
                .add("creationDate", creationDate)
                .add("title", title)
                .add("imageUrl", imageUrl)
                .toString();
    }
}