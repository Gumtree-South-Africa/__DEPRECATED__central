package ca.kijiji.replyts.user_behaviour.responsiveness.model;

import com.ecg.replyts.core.runtime.csv.CsvSerializable;

import java.time.Instant;
import java.util.Objects;

public final class ResponsivenessRecord implements CsvSerializable {

    private static final long serialVersionUID = -4755850017320860832L;

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

    public int getVersion() {
        return version;
    }

    public long getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getTimeToRespondInSeconds() {
        return timeToRespondInSeconds;
    }

    public Instant getTimestamp() {
        return timestamp;
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
        return "ResponsivenessRecord{" +
                "version=" + version +
                ", userId=" + userId +
                ", conversationId='" + conversationId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", timeToRespondInSeconds=" + timeToRespondInSeconds +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public String toCsvLine() {
        return version + "," + userId + "," + conversationId + "," + messageId + "," + timeToRespondInSeconds + ","
                + (timestamp == null ? null : timestamp.toEpochMilli());
    }
}
