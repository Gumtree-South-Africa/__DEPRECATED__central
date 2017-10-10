package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractJsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class JsonToPostBoxConverter implements AbstractJsonToPostBoxConverter<ConversationThread> {
    public PostBox<ConversationThread> toPostBox(String key, String jsonString, int maxAgeDays) {
        JsonNode json = JsonObjects.parse(jsonString);
        Iterator<JsonNode> iterator = json.get("threads").iterator();

        Spliterator<JsonNode> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
        List<ConversationThread> threads = StreamSupport.stream(spliterator, false)
          .map((threadNode) -> new ConversationThread(threadNode.get("adId").asText(),
            threadNode.get("conversationId").asText(),
            parseDate(threadNode, "createdAt"),
            parseDate(threadNode, "modifiedAt"),
            parseDate(threadNode, "receivedAt"),
            threadNode.get("containsUnreadMessages").asBoolean(),
            lookupStringValue(threadNode, "previewLastMessage"),
            lookupStringValue(threadNode, "buyerName"),
            lookupStringValue(threadNode, "sellerName"),
            lookupStringValue(threadNode, "buyerId"),
            lookupStringValue(threadNode, "messageDirection")))
          .collect(Collectors.toList());

        return new PostBox<>(key, lookupLongValue(json, "newRepliesCounter"), threads, maxAgeDays);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ?
          Optional.of(threadNode.get(key).asText()) :
          Optional.empty();
    }

    private Optional<Boolean> lookupBooleanValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ?
          Optional.of(threadNode.get(key).asBoolean()) :
          Optional.empty();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ?
          Optional.of(threadNode.get(key).asLong()) :
          Optional.empty();
    }

    private DateTime parseDate(JsonNode obj, String key) {
        JsonNode jsonNode = obj.get(key);

        if (jsonNode == null) {
            return null;
        }

        return DateTime.now().withMillis(jsonNode.asLong());
    }
}
