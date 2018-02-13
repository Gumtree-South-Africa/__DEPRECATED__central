package com.ebay.ecg.bolt.domain.service.push.model;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

public abstract class PushRequester {
	protected PushHostInfo pushHostInfo;

	protected HttpHost httpHost;
		
	protected PushRequester(PushHostInfo pushHostInfo) {
		this.pushHostInfo = pushHostInfo;
        this.httpHost = new HttpHost(pushHostInfo.getPushHost(), 443, "https");
	}

	protected PushHostInfo getPushHostInfo() {
		return pushHostInfo;
	}

	protected void setPushHostInfo(PushHostInfo pushHostInfo) {
		this.pushHostInfo = pushHostInfo;
	}

	public HttpHost getHttpHost() {
		return httpHost;
	}

	protected void setHttpHost(HttpHost httpHost) {
		this.httpHost = httpHost;
	}

	public abstract HttpResponse sendPush(HttpClient httpClient, PushMessagePayload payload,  String deviceToken, String notificationTitle, PWAInfo pwaInfo) throws Exception;
}
