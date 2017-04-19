package com.ebay.columbus.replyts2.blockeduser;


import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Created by ddallemule on 2/10/14.
 */
@Service class BlockedUserFilterFactory implements FilterFactory {

    private UserStateService userStateService;

    @Autowired public BlockedUserFilterFactory(UserStateService userStateService) {
        this.userStateService = userStateService;
    }

    @Override public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BlockedUserFilter(userStateService);
    }
}

