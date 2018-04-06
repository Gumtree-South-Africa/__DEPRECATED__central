package com.ecg.de.kleinanzeigen.replyts.bankaccountfilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

class BankAccountFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.bankaccountfilter.BankAccountFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new BankAccountFilter(BankAccountFilterConfiguration.from(configuration));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
