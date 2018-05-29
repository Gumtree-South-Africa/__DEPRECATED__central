package com.ecg.comaas.mde.postprocessor.demandreporting.client;

public class Event {
	
	private final long adId;
	private final long customerId;
	private final String publisher;
	private final String referrer;
	private final String eventType;
	
	private Event(Builder builder) {
		this.adId = builder.adId;
		this.customerId = builder.customerId;
		this.publisher = builder.publisher;
		this.referrer = builder.referrer;
		this.eventType = builder.eventType;
	}
	
	public long getAdId() {
		return adId;
	}
	public long getCustomerId() {
		return customerId;
	}
	public String getEventType() {
		return eventType;
	}
	public String getPublisher() {
		return publisher;
	}
	public String getReferrer() {
		return referrer;
	}
	
	
	public static class Builder {
		private long adId;
		private long customerId;
		private String publisher;
		private String referrer;
		private String eventType;
		
		public Builder adId(long value) {
			this.adId = value;
			return this;
		}
		
		public Builder customerId(long value) {
			this.customerId = value;
			return this;
		}
		
		public Builder publisher(String value) {
			this.publisher = value;
			return this;
		}
		
		public Builder referrer(String value) {
			this.referrer = value;
			return this;
		}
		
		public Builder eventType(String value) {
			this.eventType = value;
			return this;
		}
		
		public Event get() {
			return new Event(this);
		}
		
	}

}
