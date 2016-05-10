package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Map;

/**
 * User: acharton
 * Date: 12/18/12
 */
class UserStateConfigParser {
    private Map<String, Integer> configMap;

    public UserStateConfigParser(JsonNode jsonNode) {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        Iterator<Map.Entry<String,JsonNode>> fields = jsonNode.fields();

        Preconditions.checkArgument(fields != null && fields.hasNext(), "given config does not contain user state config elements.");

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            builder.put(next.getKey(), next.getValue().asInt());
        }

        configMap = builder.build();
    }

    public Map<String, Integer> parse() {

       return configMap;
    }
}
