package com.ecg.de.kleinanzeigen.replyts.emailaddressfilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

class EmailAddressFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.emailaddressfilter.EmailAddressFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new EmailAddressFilter(EmailAddressFilterConfiguration.from(configuration));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
