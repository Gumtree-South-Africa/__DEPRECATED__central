package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractJsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.ecg.messagecenter.util.ConverterUtils.parseDate;

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
                    lookupStringValue(threadNode, "messageDirection")
                    )
            );
        }

        return new PostBox<>(key, lookupLongValue(json, "newRepliesCounter"), threadList, maxAgeDays);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asText()) : Optional.empty();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asLong()) : Optional.empty();
    }
}
