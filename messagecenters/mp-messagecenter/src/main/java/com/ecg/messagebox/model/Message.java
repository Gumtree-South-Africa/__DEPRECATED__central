package com.ecg.messagebox.model;

import java.util.Objects;
import java.util.UUID;

public class Message {

    private UUID id;
    private String text;
    private String senderUserId;
    private MessageType type;

    public Message(UUID id, String text, String senderUserId, MessageType type) {
        this.id = id;
        this.text = text;
        this.senderUserId = senderUserId;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id)
                && Objects.equals(text, message.text)
                && Objects.equals(senderUserId, message.senderUserId)
                && type == message.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, senderUserId, type);
    }
}