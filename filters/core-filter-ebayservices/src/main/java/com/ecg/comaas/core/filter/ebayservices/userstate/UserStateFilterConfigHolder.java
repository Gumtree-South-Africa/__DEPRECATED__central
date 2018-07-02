package com.ecg.comaas.core.filter.ebayservices.userstate;

import com.ebay.marketplace.user.v1.services.UserEnum;
import com.ecg.comaas.core.filter.ebayservices.iprisk.IpRiskFilterConfigHolder;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserStateFilterConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(IpRiskFilterConfigHolder.class);

    private Map<String, Integer> userStateConfig = new HashMap<>();

    public UserStateFilterConfigHolder(JsonNode jsonNode) {
        Iterator<Map.Entry<String, JsonNode>> userStateIterator = jsonNode.fields();
        if (userStateIterator == null || !userStateIterator.hasNext()) {
            String errorMessage = "Config does not contain user states: " + jsonNode.toString();
            LOG.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        while (userStateIterator.hasNext()) {
            Map.Entry<String, JsonNode> rating = userStateIterator.next();
            userStateConfig.put(rating.getKey(), rating.getValue().asInt());
        }
    }

    public int getUserStateScore(UserEnum userState) {
        Integer userStateScore = userStateConfig.get(userState.name());
        return userStateScore != null ? userStateScore : 0;
    }
}
