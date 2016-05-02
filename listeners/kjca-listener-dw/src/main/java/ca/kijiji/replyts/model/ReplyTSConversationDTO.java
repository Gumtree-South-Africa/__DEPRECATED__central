package ca.kijiji.replyts.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.DateTime;

public class ReplyTSConversationDTO {
    private String id;
    private Long adId;
    private String replierEmail;
    private String replierAnonEmail;
    private String replierName;
    private Long replierId;
    private String posterEmail;
    private String posterAnonEmail;
    private Long posterId;
    private DateTime creationDate;
    private DateTime modificationDate;

    private ReplyTSConversationDTO() {
    }

    public ReplyTSConversationDTO(
            String id,
            Long adId,
            String replierEmail,
            String replierAnonEmail,
            String replierName,
            Long replierId,
            String posterEmail,
            String posterAnonEmail,
            Long posterId,
            DateTime creationDate,
            DateTime modificationDate
    ) {
        this.id = id;
        this.adId = adId;
        this.replierEmail = replierEmail;
        this.replierAnonEmail = replierAnonEmail;
        this.replierId = replierId;
        this.replierName = replierName;
        this.posterEmail = posterEmail;
        this.posterAnonEmail = posterAnonEmail;
        this.posterId = posterId;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
    }

    public String getId() {
        return id;
    }

    public Long getAdId() {
        return adId;
    }

    public String getReplierEmail() {
        return replierEmail;
    }

    public String getReplierAnonEmail() {
        return replierAnonEmail;
    }

    public String getReplierName() {
        return replierName;
    }

    public Long getReplierId() {
        return replierId;
    }

    public String getPosterEmail() {
        return posterEmail;
    }

    public String getPosterAnonEmail() {
        return posterAnonEmail;
    }

    public Long getPosterId() {
        return posterId;
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

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public void setReplierEmail(String replierEmail) {
        this.replierEmail = replierEmail;
    }

    public void setReplierAnonEmail(String replierAnonEmail) {
        this.replierAnonEmail = replierAnonEmail;
    }

    public void setReplierName(String replierName) {
        this.replierName = replierName;
    }

    public void setReplierId(Long replierId) {
        this.replierId = replierId;
    }

    public void setPosterEmail(String posterEmail) {
        this.posterEmail = posterEmail;
    }

    public void setPosterAnonEmail(String posterAnonEmail) {
        this.posterAnonEmail = posterAnonEmail;
    }

    public void setPosterId(Long posterId) {
        this.posterId = posterId;
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
        ReplyTSConversationDTO rhs = (ReplyTSConversationDTO) obj;
        return new EqualsBuilder()
                .append(this.id, rhs.id)
                .append(this.adId, rhs.adId)
                .append(this.replierEmail, rhs.replierEmail)
                .append(this.replierAnonEmail, rhs.replierAnonEmail)
                .append(this.replierName, rhs.replierName)
                .append(this.replierId, rhs.replierId)
                .append(this.posterEmail, rhs.posterEmail)
                .append(this.posterAnonEmail, rhs.posterAnonEmail)
                .append(this.posterId, rhs.posterId)
                .append(this.creationDate, rhs.creationDate)
                .append(this.modificationDate, rhs.modificationDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(adId)
                .append(replierEmail)
                .append(replierAnonEmail)
                .append(replierName)
                .append(replierId)
                .append(posterEmail)
                .append(posterAnonEmail)
                .append(posterId)
                .append(creationDate)
                .append(modificationDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReplyTSConversationDTO{");
        sb.append("id='").append(id).append('\'');
        sb.append(", adId=").append(adId);
        sb.append(", replierEmail='").append(replierEmail).append('\'');
        sb.append(", replierAnonEmail='").append(replierAnonEmail).append('\'');
        sb.append(", replierName='").append(replierName).append('\'');
        sb.append(", replierId=").append(replierId);
        sb.append(", posterEmail='").append(posterEmail).append('\'');
        sb.append(", posterAnonEmail='").append(posterAnonEmail).append('\'');
        sb.append(", posterId=").append(posterId);
        sb.append(", creationDate=").append(creationDate);
        sb.append(", modificationDate=").append(modificationDate);
        sb.append('}');
        return sb.toString();
    }
}
