package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

class PhoneNumberFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.phonenumberfilter.PhoneNumberFilterFactory";

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new PhoneNumberFilter(PhoneNumberFilterConfiguration.from(configuration));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
