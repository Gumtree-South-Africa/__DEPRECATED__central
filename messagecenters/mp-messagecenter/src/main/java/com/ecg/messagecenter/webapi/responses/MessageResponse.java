package com.ecg.messagecenter.webapi.responses;

import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import java.util.Objects;
import java.util.regex.Pattern;

public class MessageResponse {

    private static final Pattern REMOVE_DOUBLE_WHITESPACES = Pattern.compile("\\s+");

    private final String senderEmail;
    private final MailTypeRts boundness;
    private final String textShort;
    private final String textShortTrimmed;
    private final String receivedDate;

    // required by the diff tool
    private String messageType;

    // required by the diff tool
    private Optional<String> messageId = Optional.absent();

    public MessageResponse(String receivedDate, MailTypeRts boundness, String textShort) {
        this(receivedDate, boundness, textShort, null, null);
    }

    public MessageResponse(String messageId, String receivedDate, MailTypeRts boundness, String textShort, String senderEmail, String messageType) {
        this(receivedDate, boundness, textShort, senderEmail, messageType);
        this.messageId = Optional.of(messageId);
    }

    public MessageResponse(String receivedDate, MailTypeRts boundness, String textShort, String senderEmail, String messageType) {
        this.senderEmail = senderEmail;
        this.boundness = boundness;
        this.textShort = textShort;
        this.textShortTrimmed = REMOVE_DOUBLE_WHITESPACES.matcher(textShort).replaceAll(" ");
        this.receivedDate = receivedDate;
        this.messageType = messageType;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public MailTypeRts getBoundness() {
        return boundness;
    }

    public String getTextShort() {
        return textShort;
    }

    public String getTextShortTrimmed() {
        return textShortTrimmed;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    @JsonIgnore
    public String getMessageType() {
        return messageType;
    }

    @JsonIgnore
    public Optional<String> getMessageId() {
        return messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageResponse messageResponse = (MessageResponse) o;

        return Objects.equals(senderEmail, messageResponse.senderEmail)
                && boundness == messageResponse.boundness
                && Objects.equals(textShort, messageResponse.textShort)
                && Objects.equals(receivedDate, messageResponse.receivedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderEmail, boundness, textShort, receivedDate);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("senderEmail", senderEmail)
                .add("boundness", boundness)
                .add("textShort", textShort)
                .add("textShortTrimmed", textShortTrimmed)
                .add("receivedDate", receivedDate)
                .toString();
    }
}