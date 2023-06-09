package com.ecg.messagebox.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.datastax.driver.core.utils.UUIDs.unixTimestamp;

public class Message {

    private UUID id;
    private MessageType type;
    private MessageMetadata metadata;

    @JsonCreator
    public Message(@JsonProperty("id") UUID id,
                   @JsonProperty("type") MessageType type,
                   @JsonProperty("metadata") MessageMetadata metadata) {
        this.id = id;
        this.type = type;
        this.metadata = metadata;
    }

    public Message(UUID id, String text, String senderUserId, MessageType type) {
        this(id, type, new MessageMetadata(text, senderUserId));
    }

    public Message(UUID id, String text, String senderUserId, MessageType type, String customData) {
        this(id, type, new MessageMetadata(text, senderUserId, customData));
    }

    public Message(UUID id, String text, String senderUserId, MessageType type, String customData, Map<String, String> headers, List<Attachment> attachments) {
        this(id, type, new MessageMetadata(text, senderUserId, customData, headers, attachments));
    }

    public UUID getId() {
        return id;
    }

    @JsonIgnore
    public DateTime getReceivedDate() {
        return new DateTime(unixTimestamp(id));
    }

    public MessageType getType() {
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
                && type == message.type
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