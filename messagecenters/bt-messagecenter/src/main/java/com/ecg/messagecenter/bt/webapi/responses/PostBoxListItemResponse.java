package com.ecg.messagecenter.bt.webapi.responses;

import com.ecg.messagecenter.bt.persistence.ConversationThread;
import com.ecg.messagecenter.bt.util.MessagesDiffer;
import com.ecg.messagecenter.bt.util.MessagesResponseFactory;
import com.ecg.messagecenter.core.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.core.util.MessageCenterUtils;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PostBoxListItemResponse {
    private String email;
    private String id;
    private Long negotiationId;
    private String buyerName;
    private String sellerName;
    private String buyerEmail;
    private String adId;
    private Map<String,String> customValues;

    private ConversationRole role;
    private boolean unread;
    private List<String> attachments = Collections.emptyList();
    private ConversationState conversationState;
    private ConversationRole closeBy;

    private MessageResponse lastMessage;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(String email, ConversationThread conversationThread) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(),"Only supported for data stored as list-aggregate");

        this.email = email;
        this.unread = conversationThread.isContainsUnreadMessages();
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().isPresent() ? conversationThread.getBuyerName().get(): "";
        this.sellerName = conversationThread.getSellerName().isPresent() ? conversationThread.getSellerName().get(): "";
        this.buyerEmail = conversationThread.getBuyerId().get();
        this.adId = conversationThread.getAdId();
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
        this.customValues = conversationThread.getCustomValues().isPresent()?conversationThread.getCustomValues().get(): null;
        this.conversationState = conversationThread.getConversationState().isPresent()?conversationThread.getConversationState().get():ConversationState.ACTIVE;
        
        if (this.conversationState.equals(ConversationState.CLOSED)) {
        	this.closeBy = conversationThread.getCloseBy().get();
        }
        
        this.lastMessage = new MessageResponse(
          MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(conversationThread.getReceivedAt()),
          null,
          ConversationBoundnessFinder.boundnessForRole(this.role, conversationThread.getMessageDirection().get()),
          conversationThread.getPreviewLastMessage().get(),
          Optional.empty(),
          Collections.emptyList(),null);
    }

    @Deprecated
    public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(String email, ConversationThread conversationThread, Conversation conversationRts) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();

        response.email = email;
        response.unread = conversationThread.isContainsUnreadMessages();
        response.id = conversationRts.getId();
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.buyerEmail = conversationRts.getBuyerId();
        response.adId = conversationRts.getAdId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);
        response.customValues = conversationThread.getCustomValues().isPresent()?conversationThread.getCustomValues().get(): null;
        response.conversationState = conversationThread.getConversationState().isPresent()?conversationThread.getConversationState().get():ConversationState.ACTIVE;

        MessagesResponseFactory factory = new MessagesResponseFactory(new MessagesDiffer());

        return factory.latestMessage(email, conversationRts).map(lastMessage -> {
            response.lastMessage = lastMessage;

            return response;
        });
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getAdId() {
        return adId;
    }

    public String getReceivedDate() {
        return lastMessage.getReceivedDate();
    }

    public String getSenderEmail() {
        return lastMessage.getSenderEmail();
    }
    
    public ConversationState getConversationState(){
    	return conversationState;
    }

    public ConversationRole getCloseBy(){
    	return closeBy;
    }

    public ConversationRole getRole() {
        return role;
    }

    public MailTypeRts getBoundness() {
        return lastMessage.getBoundness();
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getTextShortTrimmed() {
        return lastMessage.getTextShortTrimmed();
    }

    public boolean isUnread() {
        return unread;
    }

    public List<String> getAttachments() {
        return attachments == null ? Collections.emptyList() : attachments;
    }
    
    public Map<String, String> getCustomValues() {
        return customValues;
    }
}