package com.ecg.comaas.core.filter.bankaccount;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class BankAccountFilterFactory implements FilterFactory {

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
