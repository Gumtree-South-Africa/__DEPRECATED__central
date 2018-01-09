package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PostBoxSingleConversationThreadResponse {
    private String id;
    private ConversationRole role;
    private String buyerEmail;
    private String buyerAnonymousEmail;
    private String sellerEmail;
    private String sellerAnonymousEmail;
    private String buyerName;
    private String sellerName;
    private String adId;
    private List<MessageResponse> messages = new ArrayList<MessageResponse>();
    private long numUnread;
    private String negotiationId;
    private ConversationState conversationState;
    private ConversationRole closeBy;
    private Map<String,String> customValues;

    private PostBoxSingleConversationThreadResponse() {
    }

    public static Optional<PostBoxSingleConversationThreadResponse> create(long numUnread, String email, Conversation conversationRts, MailCloakingService mailCloakingService, Map<String, String> customValues) {
    	PostBoxSingleConversationThreadResponse response = new PostBoxSingleConversationThreadResponse();

        response.id = conversationRts.getId();
        response.negotiationId = conversationRts.getCustomValues().get("negotiationid");
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);
        response.buyerName = conversationRts.getCustomValues().get("buyer-name") == null ? "" : conversationRts.getCustomValues().get("buyer-name");
        response.sellerName = conversationRts.getCustomValues().get("seller-name") == null ? "" : conversationRts.getCustomValues().get("seller-name");
        response.adId = conversationRts.getAdId();
        response.numUnread = numUnread;
        response.buyerEmail = conversationRts.getBuyerId();
        response.conversationState = conversationRts.getState();
        
        if (response.conversationState.equals(ConversationState.CLOSED)) {
        	response.closeBy = conversationRts.isClosedBy(ConversationRole.Buyer)?ConversationRole.Buyer:ConversationRole.Seller;
        }
        
        response.buyerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversationRts).getAddress();
        response.sellerEmail = conversationRts.getSellerId();
        response.sellerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversationRts).getAddress();
        response.setCustomValues(customValues);

        MessagesResponseFactory messagesFactory = new MessagesResponseFactory(new MessagesDiffer());

        return messagesFactory.create(email, conversationRts, conversationRts.getMessages()).map(messages -> {
            response.messages = messages;

            return response;
        });
    }

    public long getNumUnread() {
        return numUnread;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getId() {
        return id;
    }

    public ConversationRole getRole() {
        return role;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getAdId() {
        return adId;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public ConversationState getConversationState(){
    	return conversationState;
    }
    
    public ConversationRole getCloseBy(){
    	return closeBy;
    }
    
    public String getSellerEmail() {
        return sellerEmail;
    }

    public Long getNegotiationId() {
        return negotiationId == null ? null : Long.valueOf(negotiationId);
    }

	public String getBuyerAnonymousEmail() {
		return buyerAnonymousEmail;
	}

	public void setBuyerAnonymousEmail(String buyerAnonymousEmail) {
		this.buyerAnonymousEmail = buyerAnonymousEmail;
	}

	public String getSellerAnonymousEmail() {
		return sellerAnonymousEmail;
	}

	public void setSellerAnonymousEmail(String sellerAnonymousEmail) {
		this.sellerAnonymousEmail = sellerAnonymousEmail;
	}

	public Map<String,String> getCustomValues() {
		return customValues;
	}

	public void setCustomValues(Map<String, String> customValues) {
		this.customValues = customValues;
	}
}