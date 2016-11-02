package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractJsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class JsonToPostBoxConverter implements AbstractJsonToPostBoxConverter<ConversationThread> {
    @Override
    public PostBox<ConversationThread> toPostBox(String key, String jsonString, int maxAgeDays) {
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
                    lookupStringValue(threadNode, "sellerId"),
                    lookupStringValue(threadNode, "messageDirection"),
                    lookupStringValue(threadNode, "robot"),
                    lookupStringValue(threadNode, "offerId"),
                    lookupListValue(threadNode, "lastMessageAttachments"),
                    lookupStringValue(threadNode, "lastMessageId"))
            );
        }

        return new PostBox<>(key, lookupLongValue(json, "newRepliesCounter"), threadList, maxAgeDays);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asText()) : Optional.<String>empty();
    }

    private Optional<Boolean> lookupBooleanValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asBoolean()) : Optional.<Boolean>empty();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asLong()) : Optional.<Long>empty();
    }

    private List<String> lookupListValue(JsonNode threadNode, String key) {
        final List<String> stringValues = new ArrayList<>();
        if (threadNode.has(key) && threadNode.get(key).isArray()) {
            final ArrayNode array = (ArrayNode) threadNode.get(key);
            final Iterator<JsonNode> jsonNodeIterator = array.iterator();
            while (jsonNodeIterator.hasNext()) {
                stringValues.add(jsonNodeIterator.next().asText());
            }
        }
        return stringValues;
     }

    private DateTime parseDate(JsonNode obj, String key) {
        JsonNode jsonNode = obj.get(key);

        if (jsonNode == null) {
            return null;
        }

        return DateTime.now().withMillis(jsonNode.asLong());
    }
}
