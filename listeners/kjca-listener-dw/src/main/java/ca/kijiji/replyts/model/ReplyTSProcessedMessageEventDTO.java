package ca.kijiji.replyts.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ReplyTSProcessedMessageEventDTO {
    private ReplyTSMessageDTO message;
    private ReplyTSConversationDTO conversation;

    private ReplyTSProcessedMessageEventDTO() {
    }

    public ReplyTSProcessedMessageEventDTO(ReplyTSMessageDTO message, ReplyTSConversationDTO conversation) {
        this.message = message;
        this.conversation = conversation;
    }

    public ReplyTSMessageDTO getMessage() {
        return message;
    }

    public ReplyTSConversationDTO getConversation() {
        return conversation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ReplyTSProcessedMessageEventDTO rhs = (ReplyTSProcessedMessageEventDTO) obj;
        return new EqualsBuilder()
                .append(this.message, rhs.message)
                .append(this.conversation, rhs.conversation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(message)
                .append(conversation)
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReplyTSProcessedMessageEventDTO{");
        sb.append("message=").append(message);
        sb.append(", conversation=").append(conversation);
        sb.append('}');
        return sb.toString();
    }
}
