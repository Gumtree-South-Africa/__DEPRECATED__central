package com.ecg.de.mobile.replyts.fsbo.fraud;

import com.ecg.de.mobile.replyts.fsbo.fraud.broker.MessageBrokerClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



@Component
class FsboFraudFilterFactory implements FilterFactory {

	public static final String IDENTIFIER = "com.ecg.de.mobile.replyts.fsbo.fraud.FsboFraudFilterFactory";

    private final AdChecker adChecker;
	private final MessageBrokerClient messageBrokerClient;


	@Autowired
    public FsboFraudFilterFactory(AdChecker adChecker, MessageBrokerClient messageBrokerClient) {
        this.adChecker = adChecker;
        this.messageBrokerClient = messageBrokerClient;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new FsboFraudFilter(adChecker,messageBrokerClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
