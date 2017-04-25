package com.ecg.replyts.core.api.model.conversation;

import com.ecg.replyts.core.api.util.Assert;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Contains modification date for a conversation.
 */
public class ConversationModificationDate {

    private final String conversationId;
    private final Date modificationDate;
    private final Long modificationTimestamp;

    public ConversationModificationDate(String conversationId, Date modificationDate, Long modificationTimestamp) {
        Assert.notNull(conversationId);
        Assert.notNull(modificationDate);
        this.conversationId = conversationId;
        this.modificationDate = modificationDate;
        this.modificationTimestamp = modificationTimestamp;
    }

    public ConversationModificationDate(String conversationId, DateTime modificationDateTime, Long modificationTimestamp) {
        Assert.notNull(conversationId);
        Assert.notNull(modificationDateTime);
        this.conversationId = conversationId;
        this.modificationDate = modificationDateTime.toDate();
        this.modificationTimestamp = modificationTimestamp;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public DateTime getModificationDateTime() {
        return new DateTime(modificationDate);
    }

    public Long getModificationTimestamp() {
        return modificationTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationModificationDate that = (ConversationModificationDate) o;
        return Objects.equals(conversationId, that.conversationId) &&
                Objects.equals(modificationDate, that.modificationDate) &&
                Objects.equals(modificationTimestamp, that.modificationTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, modificationDate, modificationTimestamp);
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss");
        return "ConversationModificationDate{" +
                "conversationId='" + conversationId + '\'' +
                ", modificationDate=" + dateFormatter.format(modificationDate) +
                ", modificationTimestamp=" + dateFormatter.format(modificationTimestamp) +
                '}';
    }
}
