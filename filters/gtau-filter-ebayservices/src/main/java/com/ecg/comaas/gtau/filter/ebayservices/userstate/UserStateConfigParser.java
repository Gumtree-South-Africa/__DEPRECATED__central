package com.ecg.comaas.gtau.filter.ebayservices.userstate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Map;

class UserStateConfigParser {
    private Map<String, Integer> configMap;

    UserStateConfigParser(JsonNode jsonNode) {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        Iterator<Map.Entry<String,JsonNode>> fields = jsonNode.fields();

        Preconditions.checkArgument(fields != null && fields.hasNext(), "given config does not contain user state config elements.");

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            builder.put(next.getKey(), next.getValue().asInt());
        }

        configMap = builder.build();
    }

    Map<String, Integer> parse() {
       return configMap;
    }
}
