package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.util.List;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:23
 *
 * @author maldana@ebay.de
 */
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
                            lookupStringValue(threadNode, "buyerName"),
                            lookupStringValue(threadNode, "sellerName"),
                            lookupStringValue(threadNode, "buyerId"),
                            lookupStringValue(threadNode, "messageDirection"),
                            lookupLongValue(threadNode, "negotiationId"),
                            lookupLongValue(threadNode, "userIdBuyer"),
                            lookupLongValue(threadNode, "userIdSeller"))
            );
        }

        return new PostBox(key, lookupLongValue(json, "newRepliesCounter"), threadList);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asText()) : Optional.<String>absent();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asLong()) : Optional.<Long>absent();
    }

    private DateTime parseDate(JsonNode obj, String key) {
        JsonNode jsonNode = obj.get(key);

        if (jsonNode == null) {
            return null;
        }

        return DateTime.now().withMillis(jsonNode.asLong());
    }
}
