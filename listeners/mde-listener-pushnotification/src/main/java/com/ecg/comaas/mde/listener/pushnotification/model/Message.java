package com.ecg.comaas.mde.listener.pushnotification.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

import java.util.Objects;
import java.util.UUID;

import static com.datastax.driver.core.utils.UUIDs.unixTimestamp;

public class Message {

    private UUID id;
    private String type;
    private MessageMetadata metadata;

    @JsonCreator
    public Message(@JsonProperty(value = "id", required = true) UUID id,
                   @JsonProperty(value = "type", required = true) String type,
                   @JsonProperty("metadata") MessageMetadata metadata) {
        this.id = id;
        this.type = type;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public DateTime getReceivedDate() {
        return new DateTime(unixTimestamp(id));
    }

    public String getType() {
        return type;
    }

    public MessageMetadata getMetadata() {
        return metadata;
    }

    @JsonIgnore
    public String getText() {
        return metadata.getText();
    }

    @JsonIgnore
    public String getSenderUserId() {
        return metadata.getSenderUserId();
    }

    @JsonIgnore
    public String getCustomData() {
        return metadata.getCustomData();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id)
                && Objects.equals(type, message.type)
                && Objects.equals(metadata, message.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, metadata);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("receivedDate", getReceivedDate())
                .add("type", type)
                .add("metadata", metadata)
                .toString();
    }
}