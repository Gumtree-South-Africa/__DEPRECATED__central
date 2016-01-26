package com.ecg.replyts.core.api.webapi.model.imp;

import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.api.webapi.model.ConversationRtsStatus;
import com.ecg.replyts.core.api.webapi.model.MessageRts;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ConversationRtsRest implements ConversationRts, Serializable {

    private String id;

    private String buyer;
    private String buyerAnonymousEmail;

    private String seller;
    private String sellerAnonymousEmail;
    private ConversationRtsStatus status;
    private String adId;

    private Date creationDate;
    private Date lastUpdateDate;

    private Map<String, String> conversationHeaders;

    private List<MessageRts> messages;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBuyerAnonymousEmail() {
        return buyerAnonymousEmail;
    }

    public void setBuyerAnonymousEmail(String buyerAnonymousEmail) {
        this.buyerAnonymousEmail = buyerAnonymousEmail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSellerAnonymousEmail() {
        return sellerAnonymousEmail;
    }

    public void setSellerAnonymousEmail(String sellerAnonymousEmail) {
        this.sellerAnonymousEmail = sellerAnonymousEmail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversationRtsStatus getStatus() {
        return status;
    }

    public void setStatus(ConversationRtsStatus status) {
        this.status = status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getConversationHeaders() {
        return conversationHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConversationHeader(String key) {
        if (!hasConversationHeadersAttached())
            return null;
        return conversationHeaders.get(key);
    }

    public void setConversationHeaders(Map<String, String> conversationHeaders) {
        this.conversationHeaders = conversationHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MessageRts> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageRts> messages) {
        this.messages = messages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasMassagesAttached() {
        return messages != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasConversationHeadersAttached() {
        return conversationHeaders != null;
    }

}
