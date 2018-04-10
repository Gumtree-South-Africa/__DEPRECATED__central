package com.ecg.comaas.mde.filter.fsbofraud;

import com.ecg.comaas.mde.filter.fsbofraud.broker.MessageBrokerClient;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

class FsboFraudFilterFactory implements FilterFactory {

    private static final String IDENTIFIER = "com.ecg.de.mobile.replyts.fsbo.fraud.FsboFraudFilterFactory";

    private final AdChecker adChecker;
    private final MessageBrokerClient messageBrokerClient;

    public FsboFraudFilterFactory(AdChecker adChecker, MessageBrokerClient messageBrokerClient) {
        this.adChecker = adChecker;
        this.messageBrokerClient = messageBrokerClient;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new FsboFraudFilter(adChecker, messageBrokerClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
