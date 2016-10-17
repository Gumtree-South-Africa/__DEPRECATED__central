package com.ecg.replyts.core.api.model.user.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

/**
 * Base class for all user events.
 */
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EmailPreferenceEvent.class, name = "EmailPreferenceEvent"),
        @JsonSubTypes.Type(value = BlockedUserEvent.class, name = "BlockedUserEvent")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class UserEvent {

    private final DateTime date = new DateTime();
    /**
     * marks the version of this data format. can be used in future to handle old and new data formats.
     */
    @JsonProperty(required = false)
    private int formatVer = 1; // NOSONAR

    public DateTime getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEvent userEvent = (UserEvent) o;
        return Objects.equals(date, userEvent.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", date.toString())
                .add("ver", formatVer).toString();
    }
}
