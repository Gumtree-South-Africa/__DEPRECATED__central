package com.ecg.comaas.mde.filter.fsbofraud.broker;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public interface MessageBrokerClient {
	
	public void messageSend(MessageProcessingContext messageProcessingContext, long adId);

}
