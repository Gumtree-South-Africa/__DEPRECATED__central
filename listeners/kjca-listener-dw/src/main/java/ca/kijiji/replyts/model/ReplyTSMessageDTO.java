package ca.kijiji.replyts.model;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.DateTime;

public class ReplyTSMessageDTO {
    private String id;
    private String conversationId;
    private String httpHeaderId;
    private String senderIpAddress;
    private String messageOrigin;
    private Type type;
    private MessageStatus messageStatus;
    private ModerationStatus moderationStatus;
    private boolean attachment;
    private boolean blockedEmail;
    private boolean blockedUser;
    private DateTime sentDate;
    private DateTime approvedDate;
    private DateTime rejectedDate;
    private DateTime creationDate;
    private DateTime modificationDate;

    private ReplyTSMessageDTO() {
    }

    public ReplyTSMessageDTO(
            String id,
            String conversationId,
            String httpHeaderId,
            String senderIpAddress,
            String messageOrigin,
            Type type,
            MessageStatus messageStatus,
            ModerationStatus moderationStatus,
            boolean attachment,
            boolean blockedEmail,
            boolean blockedUser,
            DateTime sentDate,
            DateTime approvedDate,
            DateTime rejectedDate,
            DateTime creationDate,
            DateTime modificationDate
    ) {
        this.id = id;
        this.conversationId = conversationId;
        this.httpHeaderId = httpHeaderId;
        this.senderIpAddress = senderIpAddress;
        this.messageOrigin = messageOrigin;
        this.type = type;
        this.messageStatus = messageStatus;
        this.moderationStatus = moderationStatus;
        this.attachment = attachment;
        this.blockedEmail = blockedEmail;
        this.blockedUser = blockedUser;
        this.sentDate = sentDate;
        this.approvedDate = approvedDate;
        this.rejectedDate = rejectedDate;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getHttpHeaderId() {
        return httpHeaderId;
    }

    public String getSenderIpAddress() {
        return senderIpAddress;
    }

    public String getMessageOrigin() {
        return messageOrigin;
    }

    public Type getType() {
        return type;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public boolean isAttachment() {
        return attachment;
    }

    public boolean isBlockedEmail() {
        return blockedEmail;
    }

    public boolean isBlockedUser() {
        return blockedUser;
    }

    public DateTime getSentDate() {
        return sentDate;
    }

    public DateTime getApprovedDate() {
        return approvedDate;
    }

    public DateTime getRejectedDate() {
        return rejectedDate;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public DateTime getModificationDate() {
        return modificationDate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setHttpHeaderId(String httpHeaderId) {
        this.httpHeaderId = httpHeaderId;
    }

    public void setSenderIpAddress(String senderIpAddress) {
        this.senderIpAddress = senderIpAddress;
    }

    public void setMessageOrigin(String messageOrigin) {
        this.messageOrigin = messageOrigin;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public void setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public void setAttachment(boolean attachment) {
        this.attachment = attachment;
    }

    public void setBlockedEmail(boolean blockedEmail) {
        this.blockedEmail = blockedEmail;
    }

    public void setBlockedUser(boolean blockedUser) {
        this.blockedUser = blockedUser;
    }

    public void setSentDate(DateTime sentDate) {
        this.sentDate = sentDate;
    }

    public void setApprovedDate(DateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public void setRejectedDate(DateTime rejectedDate) {
        this.rejectedDate = rejectedDate;
    }

    public void setCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
    }

    public void setModificationDate(DateTime modificationDate) {
        this.modificationDate = modificationDate;
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
        ReplyTSMessageDTO rhs = (ReplyTSMessageDTO) obj;
        return new EqualsBuilder()
                .append(this.id, rhs.id)
                .append(this.conversationId, rhs.conversationId)
                .append(this.httpHeaderId, rhs.httpHeaderId)
                .append(this.senderIpAddress, rhs.senderIpAddress)
                .append(this.messageOrigin, rhs.messageOrigin)
                .append(this.type, rhs.type)
                .append(this.messageStatus, rhs.messageStatus)
                .append(this.moderationStatus, rhs.moderationStatus)
                .append(this.attachment, rhs.attachment)
                .append(this.blockedEmail, rhs.blockedEmail)
                .append(this.blockedUser, rhs.blockedUser)
                .append(this.sentDate, rhs.sentDate)
                .append(this.approvedDate, rhs.approvedDate)
                .append(this.rejectedDate, rhs.rejectedDate)
                .append(this.creationDate, rhs.creationDate)
                .append(this.modificationDate, rhs.modificationDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(conversationId)
                .append(httpHeaderId)
                .append(senderIpAddress)
                .append(messageOrigin)
                .append(type)
                .append(messageStatus)
                .append(moderationStatus)
                .append(attachment)
                .append(blockedEmail)
                .append(blockedUser)
                .append(sentDate)
                .append(approvedDate)
                .append(rejectedDate)
                .append(creationDate)
                .append(modificationDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReplyTSMessageDTO{");
        sb.append("id='").append(id).append('\'');
        sb.append(", conversationId='").append(conversationId).append('\'');
        sb.append(", httpHeaderId=").append(httpHeaderId);
        sb.append(", senderIpAddress=").append(senderIpAddress);
        sb.append(", messageOrigin=").append(messageOrigin);
        sb.append(", type=").append(type);
        sb.append(", messageStatus=").append(messageStatus);
        sb.append(", moderationStatus=").append(moderationStatus);
        sb.append(", attachment=").append(attachment);
        sb.append(", blockedEmail=").append(blockedEmail);
        sb.append(", blockedUser=").append(blockedUser);
        sb.append(", sentDate=").append(sentDate);
        sb.append(", approvedDate=").append(approvedDate);
        sb.append(", rejectedDate=").append(rejectedDate);
        sb.append(", creationDate=").append(creationDate);
        sb.append(", modificationDate=").append(modificationDate);
        sb.append('}');
        return sb.toString();
    }

    public enum Type {
        REPLIER_TO_POSTER(1),
        POSTER_TO_REPLIER(2),;

        private int id;

        Type(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum MessageStatus {
        SENT(1),
        HELD(2),
        DROPPED(3),;

        private int id;

        MessageStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum ModerationStatus {
        UNCHECKED(1),
        GOOD(2),
        BAD(3),
        TIMED_OUT(4),;

        private int id;

        ModerationStatus(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
