package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ebay.marketplace.security.v1.services.IPBadLevel;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IpRiskFilterConfigHolder {

    private static final Logger LOG = LoggerFactory.getLogger(IpRiskFilterConfigHolder.class);

    private final Map<String, Integer> ipRatingConfig = new HashMap<>();

    public IpRiskFilterConfigHolder(JsonNode jsonNode) {
        Iterator<Map.Entry<String, JsonNode>> ratingsIterator = jsonNode.fields();
        if (ratingsIterator == null || !ratingsIterator.hasNext()) {
            String errorMessage = "Config does not contain ratings: " + jsonNode.toString();
            LOG.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        while (ratingsIterator.hasNext()) {
            Map.Entry<String, JsonNode> rating = ratingsIterator.next();
            ipRatingConfig.put(rating.getKey(), rating.getValue().asInt());
        }
    }

    public int getRating(IPBadLevel ipBadLevel) {
        Integer ipRating = ipRatingConfig.get(ipBadLevel.name());
        return ipRating != null ? ipRating : 0;
    }
}
