package com.ecg.messagecenter.core.persistence.simple;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ConversationThreadModificationDate {
    private final String postboxId;
    private final String conversationThreadId;
    private final Date modificationDate;
    private final Date roundedModificationDate;

    public ConversationThreadModificationDate(String postboxId, String conversationThreadId, Date modificationDate, Date roundedModificationDate) {
        this.postboxId = postboxId;
        this.conversationThreadId = conversationThreadId;
        this.modificationDate = modificationDate;
        this.roundedModificationDate = roundedModificationDate;
    }

    public String getPostboxId() {
        return postboxId;
    }

    public String getConversationThreadId() {
        return conversationThreadId;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public Date getRoundedModificationDate() {
        return roundedModificationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationThreadModificationDate that = (ConversationThreadModificationDate) o;
        return Objects.equals(postboxId, that.postboxId) &&
          Objects.equals(conversationThreadId, that.conversationThreadId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postboxId, conversationThreadId);
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss");
        return "ConversationThreadModificationDate{" +
          "postboxId='" + postboxId + '\'' +
          ", conversationThreadId='" + conversationThreadId + '\'' +
          ", modificationDate=" + dateFormatter.format(modificationDate) +
          ", roundedModificationDate=" + dateFormatter.format(roundedModificationDate) +
        '}';
    }
}
