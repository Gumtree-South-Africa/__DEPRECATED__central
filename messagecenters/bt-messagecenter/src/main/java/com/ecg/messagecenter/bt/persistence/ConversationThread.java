package com.ecg.messagecenter.bt.persistence;

import java.util.Map;
import java.util.Optional;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true, value = "containsUnreadMessages")
public class ConversationThread extends AbstractConversationThread {
    private Optional<ConversationState> conversationState;
    private Optional<ConversationRole> closeBy;

    private Optional<Map<String, String>> customValues;

    public ConversationThread(
      @JsonProperty("adId") String adId,
      @JsonProperty("conversationId") String conversationId,
      @JsonProperty("createdAt") DateTime createdAt,
      @JsonProperty("modifiedAt") DateTime modifiedAt,
      @JsonProperty("receivedAt") DateTime receivedAt,
      @JsonProperty("containsUnreadMessages") boolean containsUnreadMessages,
      @JsonProperty("previewLastMessage") java.util.Optional<String> previewLastMessage,
      @JsonProperty("buyerName") java.util.Optional<String> buyerName,
      @JsonProperty("sellerName") java.util.Optional<String> sellerName,
      @JsonProperty("buyerId") java.util.Optional<String> buyerId,
      @JsonProperty("messageDirection") java.util.Optional<String> messageDirection,
      @JsonProperty("conversationState") java.util.Optional<ConversationState> conversationState,
      @JsonProperty("closeBy") java.util.Optional<ConversationRole> closeBy,
      @JsonProperty("customValue") java.util.Optional<Map<String, String>> customValues) {
        super(
          adId,
          conversationId,
          createdAt,
          modifiedAt,
          receivedAt,
          containsUnreadMessages,
          previewLastMessage,
          buyerName,
          sellerName,
          buyerId,
          messageDirection
        );

        this.conversationState = conversationState;
        this.closeBy = closeBy;
        this.customValues = customValues;
    }

    @Override
    public ConversationThread newReadConversation() {
        return new ConversationThread(adId, conversationId, createdAt, DateTime.now(), receivedAt, false, previewLastMessage, buyerName, sellerName, buyerId, messageDirection, conversationState, closeBy, customValues);
    }

    public Optional<Map<String, String>> getCustomValues() {
        return customValues;
    }

    public Optional<ConversationState> getConversationState() {
        return conversationState;
    }

    public void setConversationState(Optional<ConversationState> conversationState) {
        this.conversationState = conversationState;
    }

    public Optional<ConversationRole> getCloseBy() {
        return closeBy;
    }

    public void setCloseBy(Optional<ConversationRole> closeBy) {
        this.closeBy = closeBy;
    }

    // XXX: Original equals and hashCode only compared super() fields

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
          .add("adId", adId)
          .add("conversationId", conversationId)
          .add("createdAt", createdAt)
          .add("modifiedAt", modifiedAt)
          .add("containsUnreadMessages", containsUnreadMessages)
          .toString();
    }
}