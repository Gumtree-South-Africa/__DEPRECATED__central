package com.ebay.ecg.bolt.domain.service.push.model;

public class PushHostInfo {
    private String pushHost;

    private String authHeader;

    private String provider;

	public PushHostInfo(String pushHost, String authHeader, String provider) {
		super();

		this.pushHost = pushHost;
		this.authHeader = authHeader;
		this.provider = provider;
	}

	public String getPushHost() {
		return pushHost;
	}
	
	public String getAuthHeader() {
		return authHeader;
	}
	
	public String getProvider() {
		return provider;
	} 
}
