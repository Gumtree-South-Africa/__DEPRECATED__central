package com.ecg.de.mobile.replyts.comafilterservice.filters;

import java.util.Date;

public class ContactMessage {

    private String conversationId;

    private String fromUserId;

    private String buyerMailAddress;

    private PhoneNumber buyerPhoneNumber;

    private String message;

    private String siteId;

    private String sellerType;

    private String ipAddressV4V6;

    private Date messageCreatedTime;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getBuyerMailAddress() {
        return buyerMailAddress;
    }

    public void setBuyerMailAddress(String buyerMailAddress) {
        this.buyerMailAddress = buyerMailAddress;
    }

    public PhoneNumber getBuyerPhoneNumber() {
		return buyerPhoneNumber;
	}

	public void setBuyerPhoneNumber(PhoneNumber buyerPhoneNumber) {
		this.buyerPhoneNumber = buyerPhoneNumber;
	}

	public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getSellerType() {
        return sellerType;
    }

    public void setSellerType(String sellerType) {
        this.sellerType = sellerType;
    }

    public String getIpAddressV4V6() {
        return ipAddressV4V6;
    }

    public void setIpAddressV4V6(String ipAddressV4V6) {
        this.ipAddressV4V6 = ipAddressV4V6;
    }

    public Date getMessageCreatedTime() {
        return messageCreatedTime;
    }

    public void setMessageCreatedTime(Date messageCreatedTime) {
        this.messageCreatedTime = messageCreatedTime;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    @Override
	public String toString() {
		return "ContactMessage [conversationId=" + conversationId
                + ", fromUserId=" + fromUserId
                + ", buyerMailAddress=" + buyerMailAddress
				+ ", buyerPhoneNumber=" + buyerPhoneNumber + ", message="
				+ message + ", siteId=" + siteId + ", sellerType=" + sellerType
				+ ", ipAddressV4V6=" + ipAddressV4V6 + ", messageCreatedTime="
				+ messageCreatedTime + "]";
	}
    
    
}
