package com.ecg.messagecenter.pushmessage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class NotificationRequest {
	
	@JsonSerialize
	@JsonDeserialize
	private String toEmail;
	
	@JsonSerialize
	@JsonDeserialize
	private String message;
	
	@JsonSerialize
	@JsonDeserialize
	private Meta meta;
	
	public String getToEmail() {
		return toEmail;
	}
	public void setToEmail(String toEmail) {
		this.toEmail = toEmail;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Meta getMeta() {
		return meta;
	}
	public void setMeta(Meta meta) {
		this.meta = meta;
	}	
}
