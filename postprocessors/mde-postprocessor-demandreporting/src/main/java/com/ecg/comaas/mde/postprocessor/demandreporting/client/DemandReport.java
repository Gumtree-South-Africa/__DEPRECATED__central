package com.ecg.comaas.mde.postprocessor.demandreporting.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class DemandReport implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final long total;
	private final Map<String, Long> perPublisher;
	private final Map<String, Long> perReferrer;
	
	private DemandReport(Builder builder) {
		this.total = builder.total;
		this.perPublisher = builder.perPublisher;
		this.perReferrer = builder.perReferrer;
	}
	
	public Map<String, Long> getPerPublisher() {
		return perPublisher;
	}
	
	public Map<String, Long> getPerReferrer() {
		return perReferrer;
	}
	
	public long getTotal() {
		return total;
	}
	
	
	@Override
	public String toString() {
		return "DemandReport [total=" + total + ", perPublisher="
				+ perPublisher + ", perReferrer=" + perReferrer + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((perPublisher == null) ? 0 : perPublisher.hashCode());
		result = prime * result
				+ ((perReferrer == null) ? 0 : perReferrer.hashCode());
		result = prime * result + (int) (total ^ (total >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DemandReport other = (DemandReport) obj;
		if (perPublisher == null) {
			if (other.perPublisher != null)
				return false;
		} else if (!perPublisher.equals(other.perPublisher))
			return false;
		if (perReferrer == null) {
			if (other.perReferrer != null)
				return false;
		} else if (!perReferrer.equals(other.perReferrer))
			return false;
		if (total != other.total)
			return false;
		return true;
	}





	public static class Builder {
		private long total = 0;
		private Map<String, Long> perPublisher = Collections.emptyMap();
		private Map<String, Long> perReferrer = Collections.emptyMap();
		
		public Builder total(long value) {
			this.total = value;
			return this;
		}
		
		public Builder perPublisher(Map<String, Long> value) {
			this.perPublisher = value;
			return this;
		}
		
		public Builder perReferrer(Map<String, Long> value) {
			this.perReferrer = value;
			return this;
		}
		
		public DemandReport build() {
			return new DemandReport(this);
		}
		
	}

}
