package com.ecg.messagecenter.bt.pushmessage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class Meta {
	
	@JsonSerialize
	@JsonDeserialize
	private String adId;
	
	@JsonSerialize
	@JsonDeserialize
	private String receiverUserId;
	
	@JsonSerialize
	@JsonDeserialize
	private String senderDisplayName;
	
	@JsonSerialize
	@JsonDeserialize
	private String adTitle;
	
	@JsonSerialize
	@JsonDeserialize
	private String badge;
	
	@JsonSerialize
	@JsonDeserialize
	private String adThumbNail;
	
	@JsonSerialize
	@JsonDeserialize
	private String conversationId;
	
	@JsonSerialize
	@JsonDeserialize
	private String alertId;
	
	@JsonSerialize
	@JsonDeserialize
	private String notificationId;

	@JsonSerialize
	@JsonDeserialize
	private String senderId;

	public String getAdId() {
		return adId;
	}
	public void setAdId(String adId) {
		this.adId = adId;
	}
	public String getReceiverUserId() {
		return receiverUserId;
	}
	public void setReceiverUserId(String receiverUserId) {
		this.receiverUserId = receiverUserId;
	}
	public String getSenderDisplayName() {
		return senderDisplayName;
	}
	public void setSenderDisplayName(String senderDisplayName) {
		this.senderDisplayName = senderDisplayName;
	}
	public String getAdTitle() {
		return adTitle;
	}
	public void setAdTitle(String adTitle) {
		this.adTitle = adTitle;
	}
	public String getBadge() {
		return badge;
	}
	public void setBadge(String badge) {
		this.badge = badge;
	}
	public String getAdThumbNail() {
		return adThumbNail;
	}
	public void setAdThumbNail(String adThumbNail) {
		this.adThumbNail = adThumbNail;
	}
	public String getConversationId() {
		return conversationId;
	}
	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}
	public String getAlertId() {
		return alertId;
	}
	public void setAlertId(String alertId) {
		this.alertId = alertId;
	}
	public String getNotificationId() {
		return notificationId;
	}
	public void setNotificationId(String notificationId) {
		this.notificationId = notificationId;
	}
	public String getSenderId() {
		return senderId;
	}
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}
}
