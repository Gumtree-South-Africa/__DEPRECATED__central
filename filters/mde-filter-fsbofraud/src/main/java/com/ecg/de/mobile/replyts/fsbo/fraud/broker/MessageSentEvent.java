package com.ecg.de.mobile.replyts.fsbo.fraud.broker;

public class MessageSentEvent {
	
	private long adId;
	
	private String text;
	private String senderEmailAddress;
	private String senderRole;
	private String mailerInfo;
	private String assertedSenderIpAddress;
	
	/**
	 * for json
	 */
	MessageSentEvent() {
		
	}
	
	private MessageSentEvent(Builder builder) {
		this.adId = builder.adId;
		this.text = builder.text;
		this.senderEmailAddress = builder.senderEmailAddress;
		this.senderRole = builder.senderRole;
		this.mailerInfo = builder.mailerInfo;
		this.assertedSenderIpAddress = builder.assertedSenderIpAddress;
	}
	
	public long getAdId() {
		return adId;
	}
	
	public String getAssertedSenderIpAddress() {
		return assertedSenderIpAddress;
	}
	
	public String getText() {
		return text;
	}
	
	public String getSenderEmailAddress() {
		return senderEmailAddress;
	}
	
	public String getSenderRole() {
		return senderRole;
	}
	
	public String getMailerInfo() {
		return mailerInfo;
	}
	

	@Override
	public String toString() {
		return "MessageSentEvent [adId=" + adId + ", text=" + text
				+ ", senderEmailAddress=" + senderEmailAddress
				+ ", senderRole=" + senderRole + ", mailerInfo=" + mailerInfo
				+ ", assertedSenderIpAddress=" + assertedSenderIpAddress + "]";
	}


	public static class Builder {
		private long adId;
		private String text;
		private String senderEmailAddress;
		private String senderRole;
		private String mailerInfo;
		private String assertedSenderIpAddress;
		
		public Builder adId(long adId) {
			this.adId = adId;
			return this;
		}
		
		public Builder text(String value) {
			this.text = value;
			return this;
		}
		
		public Builder senderEmailAddress(String value) {
			this.senderEmailAddress = value;
			return this;
		}
		
		public Builder senderRole(String value) {
			this.senderRole = value;
			return this;
		}
		
		public Builder mailerInfo(String value) {
			this.mailerInfo = value;
			return this;
		}
		
		public Builder assertedSenderIpAddress(String value) {
			this.assertedSenderIpAddress = value;
			return this;
		}
		
		public MessageSentEvent get() {
			return new MessageSentEvent(this);
		}
		
	}

}
