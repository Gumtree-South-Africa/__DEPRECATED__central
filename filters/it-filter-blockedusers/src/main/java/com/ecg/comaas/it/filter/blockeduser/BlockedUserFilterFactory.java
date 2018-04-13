package com.ecg.comaas.it.filter.blockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class BlockedUserFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ebay.columbus.replyts2.blockeduser.BlockedUserFilterFactory";

    @Autowired
    private UserStateService userStateService;

    @Nonnull
    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new BlockedUserFilter(userStateService);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}