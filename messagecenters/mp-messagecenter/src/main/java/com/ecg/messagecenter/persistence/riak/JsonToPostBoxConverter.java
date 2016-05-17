package com.ecg.messagecenter.persistence.riak;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.util.List;

class JsonToPostBoxConverter {

    public PostBox toPostBox(String key, String jsonString) {
        JsonNode json = JsonObjects.parse(jsonString);
        ArrayNode arrayNode = (ArrayNode) json.get("threads");
        List<ConversationThread> threadList = Lists.newArrayList();
        for (JsonNode threadNode : arrayNode) {
            int unreadCount = 0;
            if (threadNode.has("numUnreadMessages")) {
                unreadCount = threadNode.get("numUnreadMessages").asInt();
            } else if (threadNode.has("containsUnreadMessages")) {
                // This is a trick to be backwards compatible and be able to read old postbox format that only
                // indicated whether the conversation had unread messages or not, instead of the number
                unreadCount = 1;
            }
            threadList.add(new ConversationThread(
                            threadNode.get("adId").asText(),
                            threadNode.get("conversationId").asText(),
                            parseDate(threadNode, "createdAt"),
                            parseDate(threadNode, "modifiedAt"),
                            parseDate(threadNode, "receivedAt"),
                            unreadCount,
                            lookupStringValue(threadNode, "previewLastMessage"),
                            lookupStringValue(threadNode, "buyerName"),
                            lookupStringValue(threadNode, "sellerName"),
                            lookupStringValue(threadNode, "buyerId"),
                            lookupStringValue(threadNode, "messageDirection"),
                            lookupLongValue(threadNode, "negotiationId"),
                            lookupLongValue(threadNode, "userIdBuyer"),
                            lookupLongValue(threadNode, "userIdSeller"),
                            lookupDateTimeValue(threadNode, "lastMessageCreatedAt")
                            )
            );
        }

        return new PostBox(key, threadList);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asText()) : Optional.<String>absent();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asLong()) : Optional.<Long>absent();
    }

    private Optional<DateTime> lookupDateTimeValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(parseDate(threadNode, key)) : Optional.<DateTime>absent();
    }

    private DateTime parseDate(JsonNode obj, String key) {
        JsonNode jsonNode = obj.get(key);

        if (jsonNode == null) {
            return null;
        }

        return DateTime.now().withMillis(jsonNode.asLong());
    }
}