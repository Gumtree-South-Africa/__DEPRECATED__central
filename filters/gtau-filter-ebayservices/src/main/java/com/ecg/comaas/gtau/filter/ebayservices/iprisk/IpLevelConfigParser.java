package com.ecg.comaas.gtau.filter.ebayservices.iprisk;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Map;

class IpLevelConfigParser {
    private Map<String, Integer> ipRatingConfig;

    IpLevelConfigParser(JsonNode jsonNode) {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        Iterator<Map.Entry<String,JsonNode>> fields = jsonNode.fields();

        Preconditions.checkArgument(fields != null && fields.hasNext(), "given config does not contain ip risk config element.");

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            builder.put(next.getKey(), next.getValue().asInt());
        }

        ipRatingConfig = builder.build();

    }

    Map<String, Integer> parse() {
        return ipRatingConfig;  //To change body of created methods use File | Settings | File Templates.
    }
}
