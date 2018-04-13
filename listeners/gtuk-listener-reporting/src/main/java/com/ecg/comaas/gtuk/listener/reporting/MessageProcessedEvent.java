package com.ecg.comaas.gtuk.listener.reporting;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class MessageProcessedEvent {

    String messageId;

    String conversationId;

    MessageDirection messageDirection;

    ConversationState conversationState;

    MessageState messageState;

    FilterResultState filterResultState;

    ModerationResultState humanResultState;

    Long adId;

    String sellerMail;

    String buyerMail;

    Integer numOfMessageInConversation;

    DateTime timestamp;

    DateTime conversationCreatedAt;

    DateTime messageReceivedAt;

    DateTime conversationLastModifiedAt;

    DateTime messageLastModifiedAt;

    Integer custcategoryid;

    String custip;

    List<FilterExecutionResult> filterResults;

    public MessageProcessedEvent(
            String messageId,
            String conversationId,
            MessageDirection messageDirection,
            ConversationState conversationState,
            MessageState messageState,
            FilterResultState filterResultState,
            ModerationResultState humanResultState,
            Long adId,
            String sellerMail,
            String buyerMail,
            Integer numOfMessageInConversation,
            DateTime timestamp,
            DateTime conversationCreatedAt,
            DateTime messageReceivedAt,
            DateTime conversationLastModifiedAt,
            DateTime messageLastModifiedAt,
            Integer custcategoryid,
            String custip,
            List<FilterExecutionResult> filterResults) {

        this.messageId = messageId;
        this.conversationId = conversationId;
        this.messageDirection = messageDirection;
        this.conversationState = conversationState;
        this.messageState = messageState;
        this.filterResultState = filterResultState;
        this.humanResultState = humanResultState;
        this.adId = adId;
        this.sellerMail = sellerMail;
        this.buyerMail = buyerMail;
        this.numOfMessageInConversation = numOfMessageInConversation;
        this.timestamp = timestamp;
        this.conversationCreatedAt = conversationCreatedAt;
        this.messageReceivedAt = messageReceivedAt;
        this.conversationLastModifiedAt = conversationLastModifiedAt;
        this.messageLastModifiedAt = messageLastModifiedAt;
        this.custcategoryid = custcategoryid;
        this.custip = custip;
        this.filterResults = filterResults;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    public ConversationState getConversationState() {
        return conversationState;
    }

    public MessageState getMessageState() {
        return messageState;
    }

    public FilterResultState getFilterResultState() {
        return filterResultState;
    }

    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    public Long getAdId() {
        return adId;
    }

    public String getSellerMail() {
        return sellerMail;
    }

    public String getBuyerMail() {
        return buyerMail;
    }

    public Integer getNumOfMessageInConversation() {
        return numOfMessageInConversation;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public DateTime getConversationCreatedAt() {
        return conversationCreatedAt;
    }

    public DateTime getMessageReceivedAt() {
        return messageReceivedAt;
    }

    public DateTime getConversationLastModifiedAt() {
        return conversationLastModifiedAt;
    }

    public DateTime getMessageLastModifiedAt() {
        return messageLastModifiedAt;
    }

    public Integer getCustomCategoryId() {
        return custcategoryid;
    }

    public String getCustomIp() {
        return custip;
    }

    public List<FilterExecutionResult> getFilterResults() {
        return filterResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MessageProcessedEvent that = (MessageProcessedEvent) o;

        if (!messageId.equals(that.messageId)) {
            return false;
        }
        if (!conversationId.equals(that.conversationId)) {
            return false;
        }
        if (!messageDirection.equals(that.messageDirection)) {
            return false;
        }
        if (!conversationState.equals(that.conversationState)) {
            return false;
        }
        if (!messageState.equals(that.messageState)) {
            return false;
        }
        if (!filterResultState.equals(that.filterResultState)) {
            return false;
        }
        if (!humanResultState.equals(that.humanResultState)) {
            return false;
        }
        if (!adId.equals(that.adId)) {
            return false;
        }
        if (!sellerMail.equals(that.sellerMail)) {
            return false;
        }
        if (!buyerMail.equals(that.buyerMail)) {
            return false;
        }
        if (!numOfMessageInConversation.equals(that.numOfMessageInConversation)) {
            return false;
        }
        if (!timestamp.equals(that.timestamp)) {
            return false;
        }
        if (!conversationCreatedAt.equals(that.conversationCreatedAt)) {
            return false;
        }
        if (!messageReceivedAt.equals(that.messageReceivedAt)) {
            return false;
        }
        if (!conversationLastModifiedAt.equals(that.conversationLastModifiedAt)) {
            return false;
        }
        if (!messageLastModifiedAt.equals(that.messageLastModifiedAt)) {
            return false;
        }
        if (custcategoryid != null ? !custcategoryid.equals(that.custcategoryid) : that.custcategoryid != null) {
            return false;
        }
        if (custip != null ? !custip.equals(that.custip) : that.custip != null) {
            return false;
        }
        return filterResults.equals(that.filterResults);
    }

    @Override
    public int hashCode() {
        int result = messageId.hashCode();
        result = 31 * result + conversationId.hashCode();
        result = 31 * result + messageDirection.hashCode();
        result = 31 * result + conversationState.hashCode();
        result = 31 * result + messageState.hashCode();
        result = 31 * result + filterResultState.hashCode();
        result = 31 * result + humanResultState.hashCode();
        result = 31 * result + adId.hashCode();
        result = 31 * result + sellerMail.hashCode();
        result = 31 * result + buyerMail.hashCode();
        result = 31 * result + numOfMessageInConversation.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + conversationCreatedAt.hashCode();
        result = 31 * result + messageReceivedAt.hashCode();
        result = 31 * result + conversationLastModifiedAt.hashCode();
        result = 31 * result + messageLastModifiedAt.hashCode();
        result = 31 * result + (custcategoryid != null ? custcategoryid.hashCode() : 0);
        result = 31 * result + (custip != null ? custip.hashCode() : 0);
        result = 31 * result + filterResults.hashCode();
        return result;
    }

    public static class Builder {
        private String messageId;
        private String conversationId;
        private MessageDirection messageDirection;
        private ConversationState conversationState;
        private FilterResultState filterResultState;
        private ModerationResultState humanResultState;
        private MessageState messageState;
        private Long adId;
        private String sellerMail;
        private String buyerMail;
        private Integer numOfMessageInConversation;
        private DateTime timestamp;
        private DateTime conversationCreatedAt;
        private DateTime messageReceivedAt;
        private DateTime conversationLastModifiedAt;
        private DateTime messageLastModifiedAt;
        private Integer custcategoryid;
        private String custip;
        private List<FilterExecutionResult> filterResults = new ArrayList<>();

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder messageDirection(MessageDirection messageDirection) {
            this.messageDirection = messageDirection;
            return this;
        }

        public Builder conversationState(ConversationState conversationState) {
            this.conversationState = conversationState;
            return this;
        }

        public Builder filterResultState(FilterResultState filterResultState) {
            this.filterResultState = filterResultState;
            return this;
        }

        public Builder humanResultState(ModerationResultState humanResultState) {
            this.humanResultState = humanResultState;
            return this;
        }

        public Builder messageState(MessageState messageState) {
            this.messageState = messageState;
            return this;
        }

        public Builder adId(Long adId) {
            this.adId = adId;
            return this;
        }

        public Builder sellerMail(String sellerMail) {
            this.sellerMail = sellerMail;
            return this;
        }

        public Builder buyerMail(String buyerMail) {
            this.buyerMail = buyerMail;
            return this;
        }

        public Builder numOfMessageInConversation(int numOfMessageInConversation) {
            this.numOfMessageInConversation = numOfMessageInConversation;
            return this;
        }

        public Builder timestamp(DateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder conversationCreatedAt(DateTime conversationCreatedAt) {
            this.conversationCreatedAt = conversationCreatedAt;
            return this;
        }

        public Builder messageReceivedAt(DateTime messageReceivedAt) {
            this.messageReceivedAt = messageReceivedAt;
            return this;
        }

        public Builder conversationLastModifiedAt(DateTime conversationLastModifiedAt) {
            this.conversationLastModifiedAt = conversationLastModifiedAt;
            return this;
        }

        public Builder messageLastModifiedAt(DateTime messageLastModifiedAt) {
            this.messageLastModifiedAt = messageLastModifiedAt;
            return this;
        }

        public Builder customCategoryId(Integer custcategoryid) {
            this.custcategoryid = custcategoryid;
            return this;
        }

        public Builder customBuyerIp(String custip) {
            this.custip = custip;
            return this;
        }

        public Builder addFilterResult(FilterExecutionResult.Builder filterExecutionResult) {
            filterResults.add(filterExecutionResult.createFilterExecutionResult());
            return this;
        }

        public MessageProcessedEvent createMessageProcessedEvent() {
            return new MessageProcessedEvent(
                    messageId,
                    conversationId,
                    messageDirection,
                    conversationState,
                    messageState,
                    filterResultState,
                    humanResultState,
                    adId,
                    sellerMail,
                    buyerMail,
                    numOfMessageInConversation,
                    timestamp,
                    conversationCreatedAt,
                    messageReceivedAt,
                    conversationLastModifiedAt,
                    messageLastModifiedAt,
                    custcategoryid,
                    custip,
                    filterResults);
        }
    }
}