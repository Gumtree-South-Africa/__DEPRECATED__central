package com.ecg.comaas.ebayk.filter.emailaddress;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
class EmailAddressFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.emailaddressfilter.EmailAddressFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new EmailAddressFilter(EmailAddressFilterConfiguration.from(configuration));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
