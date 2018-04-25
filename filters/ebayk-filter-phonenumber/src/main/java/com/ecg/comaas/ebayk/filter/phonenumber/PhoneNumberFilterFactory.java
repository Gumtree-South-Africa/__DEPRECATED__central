package com.ecg.comaas.ebayk.filter.phonenumber;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

@ComaasPlugin
@Profile(TENANT_EBAYK)
@Component
class PhoneNumberFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.phonenumberfilter.PhoneNumberFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new PhoneNumberFilter(PhoneNumberFilterConfiguration.from(configuration));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
