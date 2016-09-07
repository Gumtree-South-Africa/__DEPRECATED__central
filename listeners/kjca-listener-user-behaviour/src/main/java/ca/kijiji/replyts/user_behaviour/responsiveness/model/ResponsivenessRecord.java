package ca.kijiji.replyts.user_behaviour.responsiveness.model;

import java.time.Instant;
import java.util.Objects;

public class ResponsivenessRecord {
    private static final long serialVersionUID = 1L;

    private final int version;
    private final long userId;
    private final String conversationId;
    private final String messageId;
    private final int timeToRespondInSeconds;
    private final Instant timestamp;

    public ResponsivenessRecord(int version, long userId, String conversationId, String messageId, int timeToRespondInSeconds, Instant timestamp) {
        this.version = version;
        this.userId = userId;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.timeToRespondInSeconds = timeToRespondInSeconds;
        this.timestamp = timestamp;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getTimeToRespondInSeconds() {
        return timeToRespondInSeconds;
    }

    public long getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResponsivenessRecord that = (ResponsivenessRecord) o;
        return version == that.version &&
                userId == that.userId &&
                timeToRespondInSeconds == that.timeToRespondInSeconds &&
                Objects.equals(conversationId, that.conversationId) &&
                Objects.equals(messageId, that.messageId) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, userId, conversationId, messageId, timeToRespondInSeconds, timestamp);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResponsivenessRecord{");
        sb.append("userId=").append(userId);
        sb.append(", conversationId='").append(conversationId).append('\'');
        sb.append(", messageId='").append(messageId).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", timeToRespondInSeconds=").append(timeToRespondInSeconds);
        sb.append(", version=").append(version);
        sb.append('}');
        return sb.toString();
    }
}
