package com.ecg.messagecenter.persistence;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

import static com.ecg.messagecenter.util.ConverterUtils.parseDate;

class JsonToPostBoxConverter {

    public PostBox toPostBox(String key, String jsonString) {
        JsonNode json = JsonObjects.parse(jsonString);
        ArrayNode arrayNode = (ArrayNode) json.get("threads");
        List<ConversationThread> threadList = Lists.newArrayList();
        for (JsonNode threadNode : arrayNode) {
            threadList.add(new ConversationThread(
                    threadNode.get("adId").asText(),
                    threadNode.get("conversationId").asText(),
                    parseDate(threadNode, "createdAt"),
                    parseDate(threadNode, "modifiedAt"),
                    parseDate(threadNode, "receivedAt"),
                    threadNode.get("containsUnreadMessages").asBoolean(),
                    lookupStringValue(threadNode, "previewLastMessage"),
                    lookupStringValue(threadNode,"buyerName"),
                    lookupStringValue(threadNode,"sellerName"),
                    lookupStringValue(threadNode, "buyerId"),
                    lookupStringValue(threadNode, "messageDirection")
                    )
            );
        }

        return new PostBox(key, lookupLongValue(json, "newRepliesCounter"), threadList);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asText()) : Optional.empty();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asLong()) : Optional.empty();
    }
}
