package com.ecg.de.mobile.replyts.fsbo.fraud.broker;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public interface MessageBrokerClient {
	
	public void messageSend(MessageProcessingContext messageProcessingContext, long adId);

}
